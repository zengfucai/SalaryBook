
package com.salarybook.domain.salary

import java.math.BigDecimal
import java.math.RoundingMode

data class SalaryCalculationInput(
    val salaryType: String = "HOURLY",
    val monthlySalary: Double = 0.0,
    val hourlyRate: Double = 0.0,
    val dailyRate: Double = 0.0,
    val weeklyRate: Double = 0.0,
    val monthlyAllocationDays: Double = 0.0,
    val weeklyAllocationDays: Double = 0.0,
    val paidUnits: Double = 1.0,
    val normalMinutes: Int = 0,

    // 旧版加班字段，继续兼容
    val overtimeMinutes: Int = 0,
    val overtimeMultiplier: Double = 1.0,

    // 新版分类型加班分钟数
    val weekdayOvertimeMinutes: Int = 0,
    val restDayOvertimeMinutes: Int = 0,
    val holidayOvertimeMinutes: Int = 0,

    val weekdayOvertimeMultiplier: Double = 1.5,
    val restDayOvertimeMultiplier: Double = 2.0,
    val holidayOvertimeMultiplier: Double = 3.0,

    val standardHoursPerDay: Double = 8.0,
    val pieceCount: Double = 0.0,
    val pieceRate: Double = 0.0,
    val commissionBase: Double = 0.0,
    val commissionRate: Double = 0.0,
    val commissionPay: Double = 0.0,
    val bonus: Double = 0.0,
    val allowance: Double = 0.0,
    val nightHours: Double = 0.0,
    val nightAllowancePerHour: Double = 0.0,
    val nightAllowance: Double = 0.0,
    val deduction: Double = 0.0,
    val customIncome: Double = 0.0,
    val customDeduction: Double = 0.0
)

data class SalaryCalculationResult(
    val normalPay: Double,
    val overtimePay: Double,
    val extraPay: Double,
    val deduction: Double,
    val grossPay: Double,
    val netPay: Double,
    val basePay: Double = 0.0,
    val commissionPay: Double = 0.0,
    val piecePay: Double = 0.0,
    val normalMinutes: Int = 0,
    val overtimeMinutes: Int = 0
)

object SalaryCalculator {

    fun calculate(
        input: SalaryCalculationInput
    ): SalaryCalculationResult {

        val units = input.paidUnits.coerceAtLeast(0.0)
        val normalMinutes = input.normalMinutes.coerceAtLeast(0)

        val weekdayMinutes =
            input.weekdayOvertimeMinutes.coerceAtLeast(0)

        val restDayMinutes =
            input.restDayOvertimeMinutes.coerceAtLeast(0)

        val holidayMinutes =
            input.holidayOvertimeMinutes.coerceAtLeast(0)

        val hasSeparatedOvertime =
            weekdayMinutes + restDayMinutes + holidayMinutes > 0

        // 新版使用三种独立加班分钟数；旧版继续使用原字段
        val actualWeekdayMinutes =
            if (hasSeparatedOvertime) weekdayMinutes else 0

        val actualRestDayMinutes =
            if (hasSeparatedOvertime) restDayMinutes else 0

        val actualHolidayMinutes =
            if (hasSeparatedOvertime) holidayMinutes else 0

        val legacyOvertimeMinutes =
            if (hasSeparatedOvertime) 0
            else input.overtimeMinutes.coerceAtLeast(0)

        val totalOvertimeMinutes =
            actualWeekdayMinutes +
                    actualRestDayMinutes +
                    actualHolidayMinutes +
                    legacyOvertimeMinutes

        val monthlyDays =
            input.monthlyAllocationDays.coerceAtLeast(0.0)

        val weeklyDays =
            input.weeklyAllocationDays.coerceAtLeast(0.0)

        val hourlyBase =
            input.hourlyRate.coerceAtLeast(0.0)

        val dailyBase =
            input.dailyRate.coerceAtLeast(0.0)

        val monthlyDailyRate =
            if (monthlyDays > 0.0) {
                input.monthlySalary.coerceAtLeast(0.0) / monthlyDays
            } else {
                0.0
            }

        val weeklyDailyRate =
            if (weeklyDays > 0.0) {
                input.weeklyRate.coerceAtLeast(0.0) / weeklyDays
            } else {
                0.0
            }

        val piecePay =
            input.pieceCount.coerceAtLeast(0.0) *
                    input.pieceRate.coerceAtLeast(0.0)

        val basePay = when (input.salaryType) {
            "MONTHLY" ->
                monthlyDailyRate * units

            "HOURLY" ->
                hourlyBase * normalMinutes / 60.0

            "DAILY" ->
                dailyBase * units

            "WEEKLY" ->
                weeklyDailyRate * units

            "PIECEWORK" ->
                0.0

            "MIXED" -> {
                val monthlyPart =
                    if (monthlyDays > 0.0) monthlyDailyRate * units
                    else 0.0

                val dailyPart = dailyBase * units

                val weeklyPart =
                    if (weeklyDays > 0.0) weeklyDailyRate * units
                    else 0.0

                val hourlyPart =
                    hourlyBase * normalMinutes / 60.0

                monthlyPart + dailyPart + weeklyPart + hourlyPart
            }

            else -> 0.0
        }

        val standardHours =
            input.standardHoursPerDay.takeIf { it > 0.0 } ?: 0.0

        val overtimeHourlyRate = when (input.salaryType) {
            "HOURLY" -> hourlyBase

            "MONTHLY" ->
                if (monthlyDays > 0.0 && standardHours > 0.0) {
                    monthlyDailyRate / standardHours
                } else {
                    0.0
                }

            "DAILY" ->
                if (standardHours > 0.0) {
                    dailyBase / standardHours
                } else {
                    0.0
                }

            "WEEKLY" ->
                if (weeklyDays > 0.0 && standardHours > 0.0) {
                    weeklyDailyRate / standardHours
                } else {
                    0.0
                }

            "MIXED" -> {
                val monthlyHourly =
                    if (monthlyDays > 0.0 && standardHours > 0.0) {
                        monthlyDailyRate / standardHours
                    } else {
                        0.0
                    }

                val dailyHourly =
                    if (standardHours > 0.0) {
                        dailyBase / standardHours
                    } else {
                        0.0
                    }

                val weeklyHourly =
                    if (weeklyDays > 0.0 && standardHours > 0.0) {
                        weeklyDailyRate / standardHours
                    } else {
                        0.0
                    }

                hourlyBase +
                        monthlyHourly +
                        dailyHourly +
                        weeklyHourly
            }

            else -> 0.0
        }

        val overtimePay = if (hasSeparatedOvertime) {
            overtimeHourlyRate / 60.0 * (
                    actualWeekdayMinutes *
                            input.weekdayOvertimeMultiplier.coerceAtLeast(0.0) +
                            actualRestDayMinutes *
                            input.restDayOvertimeMultiplier.coerceAtLeast(0.0) +
                            actualHolidayMinutes *
                            input.holidayOvertimeMultiplier.coerceAtLeast(0.0)
                    )
        } else {
            overtimeHourlyRate *
                    legacyOvertimeMinutes / 60.0 *
                    input.overtimeMultiplier.coerceAtLeast(0.0)
        }

        val commissionPay =
            input.commissionPay.coerceAtLeast(0.0) +
                    input.commissionBase.coerceAtLeast(0.0) *
                    input.commissionRate.coerceAtLeast(0.0)

        val nightPay =
            input.nightAllowance.coerceAtLeast(0.0) +
                    input.nightHours.coerceAtLeast(0.0) *
                    input.nightAllowancePerHour.coerceAtLeast(0.0)

        val extraPay =
            piecePay +
                    commissionPay +
                    input.bonus.coerceAtLeast(0.0) +
                    input.allowance.coerceAtLeast(0.0) +
                    nightPay +
                    input.customIncome.coerceAtLeast(0.0)

        val deduction =
            input.deduction.coerceAtLeast(0.0) +
                    input.customDeduction.coerceAtLeast(0.0)

        val grossPay = basePay + overtimePay + extraPay
        val netPay = (grossPay - deduction).coerceAtLeast(0.0)

        return SalaryCalculationResult(
            normalPay = money(basePay),
            overtimePay = money(overtimePay),
            extraPay = money(extraPay),
            deduction = money(deduction),
            grossPay = money(grossPay),
            netPay = money(netPay),
            basePay = money(basePay),
            commissionPay = money(commissionPay),
            piecePay = money(piecePay),
            normalMinutes = normalMinutes,
            overtimeMinutes = totalOvertimeMinutes
        )
    }

    private fun money(value: Double): Double {
        return BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }
}
