export interface PlanFeatureInput {
  code: string;
  maxUsers: number;
  maxRoles: number;
  trialDays?: number | null;
  contactSales?: boolean;
}

export function planFeatures(plan: PlanFeatureInput): string[] {
  const code = plan.code.trim().toUpperCase();
  const features = [
    `Hasta ${formatLimit(plan.maxUsers)} usuarios`,
    `Hasta ${formatLimit(plan.maxRoles)} roles personalizados`
  ];

  if (code === 'BASIC') {
    features.push('Usuarios, estructura, empleados, contratos y expedientes');
    features.push('Dashboard y notificaciones');
  } else if (code === 'PRO' || code === 'PROFESSIONAL') {
    features.push('Todo lo de Basic, más turnos, asistencia y ausencias');
    features.push('Dashboard y notificaciones');
  } else if (code === 'ENTERPRISE') {
    features.push('Todos los módulos de gestión de RR. HH.');
    features.push('Incidencias, reportes, exportaciones y respaldos');
  } else {
    features.push('Acceso completo para validar la plataforma');
    features.push('Turnos, asistencia, ausencias, incidencias, reportes y respaldos');
  }

  if (code === 'DEMO' || code === 'FREE') {
    features.push(`${plan.trialDays ?? 10} días de prueba sin tarjeta`);
  } else if (plan.contactSales) {
    features.push('Condiciones comerciales y capacidad a medida');
  } else {
    features.push('Pago seguro con Stripe');
  }

  return features;
}

function formatLimit(value: number): string {
  return new Intl.NumberFormat('es-ES').format(value);
}
