package com.chirag.arthix.data.model

import androidx.room.TypeConverter

/**
 * Room [TypeConverter]s for all Arthix enums.
 *
 * Room does not persist Kotlin enums natively — these convert each enum to/from
 * its [Enum.name] String representation for storage.
 */
class EnumConverters {

    // Direction
    @TypeConverter fun toDirection(value: String): Direction = Direction.valueOf(value)
    @TypeConverter fun fromDirection(value: Direction): String = value.name

    // CaptureSource
    @TypeConverter fun toCaptureSource(value: String): CaptureSource = CaptureSource.valueOf(value)
    @TypeConverter fun fromCaptureSource(value: CaptureSource): String = value.name

    // TransactionStatus
    @TypeConverter fun toTransactionStatus(value: String): TransactionStatus = TransactionStatus.valueOf(value)
    @TypeConverter fun fromTransactionStatus(value: TransactionStatus): String = value.name

    // ConfidenceFlag
    @TypeConverter fun toConfidenceFlag(value: String): ConfidenceFlag = ConfidenceFlag.valueOf(value)
    @TypeConverter fun fromConfidenceFlag(value: ConfidenceFlag): String = value.name

    // SplitConfirmedVia
    @TypeConverter fun toSplitConfirmedVia(value: String): SplitConfirmedVia = SplitConfirmedVia.valueOf(value)
    @TypeConverter fun fromSplitConfirmedVia(value: SplitConfirmedVia): String = value.name

    // AmountLock
    @TypeConverter fun toAmountLock(value: String): AmountLock = AmountLock.valueOf(value)
    @TypeConverter fun fromAmountLock(value: AmountLock): String = value.name

    // GoalPlanType
    @TypeConverter fun toGoalPlanType(value: String): GoalPlanType = GoalPlanType.valueOf(value)
    @TypeConverter fun fromGoalPlanType(value: GoalPlanType): String = value.name

    // GoalStatus
    @TypeConverter fun toGoalStatus(value: String): GoalStatus = GoalStatus.valueOf(value)
    @TypeConverter fun fromGoalStatus(value: GoalStatus): String = value.name
}

