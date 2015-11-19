package org.apache.spark.sql.execution.datasources.parquet

import java.nio.{ByteBuffer, ByteOrder}
import java.sql.Timestamp

import org.apache.parquet.io.api.Binary
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.util.DateTimeUtils
import org.scalatest.{Assertions, FunSuite}
import org.scalatest.Assertions._

/**
 * There are a few confusions whem dealing with timestamp
 * 1. conversions between scala (Timestamp) and catalyst(Long) needs to be done correctly. See
 * org.apache.spark.sql.catalyst.CatalystTypeConverters.TimestampConverter
 * 2. Catalyst converts the Timestamp(Long) to binary in LITTLE_ENDIAN.
 * 3. the default implementation of ByteArrayBackedBinary#compareTo does not work with LITTLE_ENDIAN
 *
 */
class BinaryTest extends FunSuite {
  /**
   * @see org.apache.spark.sql.catalyst.CatalystTypeConverters.TimestampConverter
   */
  private object TimestampConverter {
    /**
     * Converts a Scala type to its Catalyst equivalent while automatically handling nulls
     * and Options.
     */
    def toCatalystImpl(scalaValue: Timestamp): Long =
      DateTimeUtils.fromJavaTimestamp(scalaValue)

    /**
     * Given a Catalyst row, convert the value at column `column` to its Scala equivalent.
     */
    def toScala(catalystValue: Any): Timestamp =
      if (catalystValue == null) null
      else DateTimeUtils.toJavaTimestamp(catalystValue.asInstanceOf[Long])

    /**
     * Given a Catalyst row, convert the value at column `column` to its Scala equivalent.
     * This method will only be called on non-null columns.
     */
    def toScalaImpl(row: InternalRow, column: Int): Timestamp =
      DateTimeUtils.toJavaTimestamp(row.getLong(column))
  }


  /**
   * @see org.apache.spark.sql.execution.datasources.parquet.CatalystRowConverter#newConverter
   * @param value
   * @return
   */
  def toTimestamp(value: Binary): Timestamp = {
    assert(
      value.length() == 12,
      "Timestamps (with nanoseconds) are expected to be stored in 12-byte long binaries, " +
        s"but got a ${value.length()}-byte binary.")

    //val buf = value.toByteBuffer.order(ByteOrder.LITTLE_ENDIAN)
    val buf = value.toByteBuffer.order(ByteOrder.BIG_ENDIAN)
    val timeOfDayNanos = buf.getLong
    val julianDay = buf.getInt
    val ts = DateTimeUtils.fromJulianDay(julianDay, timeOfDayNanos)
    TimestampConverter.toScala(ts)
  }

  /**
   * @see org.apache.spark.sql.execution.datasources.parquet.CatalystWriteSupport#makeWriter
   * @param t
   * @return
   */
  def toBinary(t: Timestamp): Binary = {
    val l = Option(t).map(TimestampConverter.toCatalystImpl(_)).getOrElse(0l)
    val (julianDay, timeOfDayNanos) = DateTimeUtils.toJulianDay(l)
    val timestampBuffer = new Array[Byte](12)
    val buf = ByteBuffer.wrap(timestampBuffer)
    //buf.order(ByteOrder.LITTLE_ENDIAN).putLong(timeOfDayNanos).putInt(julianDay)
    buf.order(ByteOrder.BIG_ENDIAN).putLong(timeOfDayNanos).putInt(julianDay)
    val b = Binary.fromByteArray(timestampBuffer)
    b
  }

  test("test TimestampConverter"){
    val t1 = new Timestamp(1)
    val l = TimestampConverter.toCatalystImpl(t1)
    val t2 = TimestampConverter.toScala(l)
    println(t1.equals(t2))
  }

  test("binary ordering") {
    val t1 = new Timestamp(1)
    val b1 = toBinary(t1)
    println(s"t=$t1, b=$b1, c=${toTimestamp(b1)}")
    val t2 = new Timestamp(2)
    val b2 = toBinary(t2)
    println(s"t=$t2, b=$b2, c=${toTimestamp(b2)}")
    val t3 = new Timestamp(3)
    val b3 = toBinary(t3)
    println(s"t=$t3, b=$b3, c=${toTimestamp(b3)}")
    val t4 = new Timestamp(4)
    val b4 = toBinary(t4)
    println(s"t=$t4, b=$b4, c=${toTimestamp(b4)}")

    /**
     * org.apache.parquet.io.api.Binary.ByteArrayBackedBinary#compareTo(byte[], int, int) does
     * not compare correctly
     */
    val cmp12 = b1.compareTo(b2)
    val cmp23 = b2.compareTo(b3)
    val cmp34 = b3.compareTo(b4)

    println(cmp12)
    println(cmp23)
    println(cmp34)
  }


}
