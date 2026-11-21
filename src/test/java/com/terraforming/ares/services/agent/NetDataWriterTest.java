package com.terraforming.ares.services.agent;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the bytes of `netdata.bin`, the corpus of tens of millions of states that the value is trained on.
 * <p>
 * Why: two silent corpus bugs cost hours. The game id was parsed as float16 in the CSV reader, which rounds every id above 2048 (50 000 games became
 * 6 682, the train/validation split leaked and the model played at 4.85% against RANDOM); and the torch export had W1/W2 transposed. The binary
 * format is the same kind of surface - an int in the wrong place or a wrong endianness still produces a file that "trains" - so its layout is parsed
 * here independently of the writer, and `ml/tests/test_train_network_v2.py` reads the same layout from the Python side.
 */
class NetDataWriterTest {

    @Test
    void the_magic_is_eight_bytes_and_identifies_the_format() {
        assertEquals(8, NetDataWriter.MAGIC.length);
        assertArrayEquals("ND2BIN01".getBytes(), NetDataWriter.MAGIC);
    }

    @Test
    void a_record_is_little_endian_int32_id_then_float16_label_and_values() {
        float[] data = new float[]{0.0f, 1.0f, 0.5f, -2.25f};
        ByteBuffer buffer = ByteBuffer.allocate(4 + 2 + 2 * data.length).order(ByteOrder.LITTLE_ENDIAN);

        // 5001 is the id the float16 parse could not hold (it rounds to 5000): it must survive exactly.
        NetDataWriter.encodeRow(buffer, 5001, 1.0f, data);

        ByteBuffer read = ByteBuffer.wrap(buffer.array()).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(5001, read.getInt(), "the game id is an int32 in the file");
        assertEquals(1.0f, Float.float16ToFloat(read.getShort()), 0.0f);
        assertEquals(0.0f, Float.float16ToFloat(read.getShort()), 0.0f);
        assertEquals(1.0f, Float.float16ToFloat(read.getShort()), 0.0f);
        assertEquals(0.5f, Float.float16ToFloat(read.getShort()), 0.0f);
        assertEquals(-2.25f, Float.float16ToFloat(read.getShort()), 0.0f);
        assertEquals(0, read.remaining(), "one record is exactly 4 + 2 + 2*dimension bytes");
    }

    @Test
    void a_label_of_a_draw_is_preserved() {
        ByteBuffer buffer = ByteBuffer.allocate(4 + 2 + 2).order(ByteOrder.LITTLE_ENDIAN);
        NetDataWriter.encodeRow(buffer, 7, 0.5f, new float[]{0.0f});
        ByteBuffer read = ByteBuffer.wrap(buffer.array()).order(ByteOrder.LITTLE_ENDIAN);
        read.getInt();
        assertEquals(0.5f, Float.float16ToFloat(read.getShort()), 0.0f);
    }
}
