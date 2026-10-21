package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSetting;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSettingKeys;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class RegionalCountrySettingsService {
    private static final Set<String> COUNTRIES = Set.of(Locale.getISOCountries());
    private final TenantSettingRepository repository;
    private final AuditTrailService audit;
    private final ObjectMapper mapper;

    public RegionalCountrySettingsService(TenantSettingRepository repository, AuditTrailService audit, ObjectMapper mapper) {
        this.repository = repository;
        this.audit = audit;
        this.mapper = mapper;
    }

    public record Holiday(String date, String name) {}
    public record Holidays(List<Holiday> holidays) {}
    public record Workday(List<String> days, String start, String end, String breakStart, String breakEnd) {}
    public record WorkdayView(List<String> days, String start, String end, String breakStart, String breakEnd,
                              int dailyMinutes, int weeklyMinutes) {}
    public record Rules(Integer contractReminderDays, Integer shiftBufferMinutes,
                        Integer attendanceToleranceMinutes, Integer absenceNoticeHours) {}

    @Transactional(readOnly = true)
    public Holidays holidays(String country) {
        return read(key(country, "holidays"), Holidays.class, new Holidays(List.of()));
    }

    @Transactional
    public Holidays saveHolidays(String country, Holidays request) {
        String key = key(country, "holidays");
        if (request == null || request.holidays() == null || request.holidays().size() > 366) {
            throw new BusinessException("La lista de feriados no es válida");
        }
        Set<LocalDate> dates = new HashSet<>();
        List<Holiday> sorted = new ArrayList<>();
        for (Holiday holiday : request.holidays()) {
            if (holiday == null || holiday.name() == null || holiday.name().trim().isEmpty()
                    || holiday.name().trim().length() > 120) {
                throw new BusinessException("El nombre del feriado no es válido");
            }
            LocalDate date = date(holiday.date());
            if (!dates.add(date)) throw new BusinessException("Ya existe un feriado en esa fecha");
            sorted.add(new Holiday(date.toString(), holiday.name().trim()));
        }
        sorted.sort(Comparator.comparing(Holiday::date));
        Holidays result = new Holidays(List.copyOf(sorted));
        save(key, country, "TENANT_REGIONAL_HOLIDAYS", result, Holidays.class, new Holidays(List.of()));
        return result;
    }

    @Transactional(readOnly = true)
    public WorkdayView workday(String country) {
        return view(read(key(country, "workday"), Workday.class,
                new Workday(List.of(), null, null, null, null)));
    }

    @Transactional
    public WorkdayView saveWorkday(String country, Workday request) {
        String key = key(country, "workday");
        WorkdayView validated = view(request);
        Workday normalized = new Workday(validated.days(), validated.start(), validated.end(),
                validated.breakStart(), validated.breakEnd());
        save(key, country, "TENANT_REGIONAL_WORKDAY", normalized, Workday.class,
                new Workday(List.of(), null, null, null, null));
        return validated;
    }

    @Transactional(readOnly = true)
    public Rules rules(String country) {
        return read(key(country, "rules"), Rules.class, new Rules(null, null, null, null));
    }

    @Transactional
    public Rules saveRules(String country, Rules request) {
        String key = key(country, "rules");
        if (request == null) throw new BusinessException("Los parámetros regionales no son válidos");
        range(request.contractReminderDays(), 0, 365, "Recordatorio de contratos");
        range(request.shiftBufferMinutes(), 0, 1440, "Intervalo entre turnos");
        range(request.attendanceToleranceMinutes(), 0, 240, "Tolerancia de asistencia");
        range(request.absenceNoticeHours(), 0, 720, "Aviso de ausencias");
        save(key, country, "TENANT_REGIONAL_RULES", request, Rules.class,
                new Rules(null, null, null, null));
        return request;
    }

    private WorkdayView view(Workday value) {
        if (value == null || value.days() == null) throw new BusinessException("La jornada no es válida");
        if (value.days().isEmpty() && value.start() == null && value.end() == null
                && value.breakStart() == null && value.breakEnd() == null) {
            return new WorkdayView(List.of(), null, null, null, null, 0, 0);
        }
        if (value.days().isEmpty() || value.days().size() > 7 || new HashSet<>(value.days()).size() != value.days().size()) {
            throw new BusinessException("Los días laborables no son válidos");
        }
        List<DayOfWeek> days = new ArrayList<>();
        try {
            for (String day : value.days()) days.add(DayOfWeek.valueOf(day));
        } catch (RuntimeException ex) {
            throw new BusinessException("Los días laborables no son válidos", ex);
        }
        days.sort(Comparator.naturalOrder());
        LocalTime start = time(value.start());
        LocalTime end = time(value.end());
        if (!start.isBefore(end)) throw new BusinessException("La hora final debe ser posterior a la inicial");
        if ((value.breakStart() == null) != (value.breakEnd() == null)) {
            throw new BusinessException("El descanso requiere inicio y fin");
        }
        int breakMinutes = 0;
        String breakStart = null, breakEnd = null;
        if (value.breakStart() != null) {
            LocalTime from = time(value.breakStart());
            LocalTime to = time(value.breakEnd());
            if (!start.isBefore(from) || !from.isBefore(to) || !to.isBefore(end)) {
                throw new BusinessException("El descanso debe estar dentro de la jornada");
            }
            breakMinutes = (int) ChronoUnit.MINUTES.between(from, to);
            breakStart = from.toString();
            breakEnd = to.toString();
        }
        int daily = (int) ChronoUnit.MINUTES.between(start, end) - breakMinutes;
        return new WorkdayView(days.stream().map(Enum::name).toList(), start.toString(), end.toString(),
                breakStart, breakEnd, daily, daily * days.size());
    }

    private static String key(String country, String section) {
        if (country == null || !COUNTRIES.contains(country)) throw new BusinessException("El país no es válido");
        return "regional.country." + country + "." + section;
    }

    private static LocalDate date(String value) {
        try {
            LocalDate parsed = LocalDate.parse(value);
            if (!parsed.toString().equals(value)) throw new IllegalArgumentException();
            return parsed;
        } catch (RuntimeException ex) {
            throw new BusinessException("La fecha del feriado no es válida", ex);
        }
    }

    private static LocalTime time(String value) {
        try {
            LocalTime parsed = LocalTime.parse(value);
            if (parsed.getSecond() != 0 || parsed.getNano() != 0) throw new IllegalArgumentException();
            return parsed;
        } catch (RuntimeException ex) {
            throw new BusinessException("La hora de la jornada no es válida", ex);
        }
    }

    private static void range(Integer value, int min, int max, String name) {
        if (value != null && (value < min || value > max)) throw new BusinessException(name + " fuera de rango");
    }

    private <T> T read(String key, Class<T> type, T fallback) {
        return repository.findByKey(key).map(row -> decode(row.settingValue(), type)).orElse(fallback);
    }

    private <T> T decode(String json, Class<T> type) {
        try {
            T value = mapper.readValue(json, type);
            if (value == null) throw new BusinessException("Configuración regional vacía");
            return value;
        } catch (JsonProcessingException ex) {
            throw new BusinessException("La configuración regional guardada no es válida", ex);
        }
    }

    private <T> void save(String key, String country, String resourceType, T value, Class<T> type, T fallback) {
        repository.requireForUpdate(TenantSettingKeys.HR_SETUP_COMPLETED);
        T before = read(key, type, fallback);
        if (Objects.equals(before, value)) return;
        TenantSetting current = repository.findByKey(key).orElse(null);
        String json;
        try {
            json = mapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("No se pudo codificar la configuración regional", ex);
        }
        repository.save(new TenantSetting(current == null ? null : current.id(), key, json,
                current == null ? null : current.createdAt(), current == null ? null : current.updatedAt()));
        audit.recordTenantEvent(AuditEventTypes.TENANT_SETTINGS_UPDATED, resourceType, country,
                Map.of("settingKey", key), before, value);
    }
}
