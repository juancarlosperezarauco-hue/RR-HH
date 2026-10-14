export interface HolidayResponse {
  id: string;              // UUID
  tenantId: string;        // UUID
  name: string;            // VARCHAR(100)
  holidayDate: string;     // YYYY-MM-DD
  isRecurring: boolean;
  scope: 'NATIONAL' | 'REGIONAL';
}
