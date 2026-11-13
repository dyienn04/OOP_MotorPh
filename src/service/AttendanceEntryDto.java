package service;

public class AttendanceEntryDto {

    private final String date;
    private final String timeIn;
    private final String timeOut;
    private final String workedHours;
    private final int lateMinutes;
    private final int overtimeMinutes;

    public AttendanceEntryDto(
            String date,
            String timeIn,
            String timeOut,
            String workedHours,
            int lateMinutes,
            int overtimeMinutes
    ) {
        this.date = date;
        this.timeIn = timeIn;
        this.timeOut = timeOut;
        this.workedHours = workedHours;
        this.lateMinutes = lateMinutes;
        this.overtimeMinutes = overtimeMinutes;
    }

    public String getDate() {
        return date;
    }

    public String getTimeIn() {
        return timeIn;
    }

    public String getTimeOut() {
        return timeOut;
    }

    public String getWorkedHours() {
        return workedHours;
    }

    public int getLateMinutes() {
        return lateMinutes;
    }

    public int getOvertimeMinutes() {
        return overtimeMinutes;
    }
}
