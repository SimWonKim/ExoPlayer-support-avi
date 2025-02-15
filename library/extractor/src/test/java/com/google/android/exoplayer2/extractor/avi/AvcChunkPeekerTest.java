/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.extractor.avi;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.testutil.FakeExtractorInput;
import com.google.android.exoplayer2.testutil.FakeTrackOutput;
import com.google.android.exoplayer2.testutil.TestUtil;
import com.google.android.exoplayer2.util.MimeTypes;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AvcChunkPeekerTest {
  private static final Format.Builder FORMAT_BUILDER_AVC = new Format.Builder().
      setSampleMimeType(MimeTypes.VIDEO_H264).
      setWidth(1280).setHeight(720).setFrameRate(24000f/1001f);

  private static final byte[] P_SLICE = {0,0,0,1,0x41,(byte)0x9A,0x13,0x36,0x21,0x3A,0x5F,
      (byte)0xFE,(byte)0x9E,0x10,0,0};

  private FakeTrackOutput fakeTrackOutput;
  private AvcChunkHandler avcChunkHandler;

  @Before
  public void before() {
    fakeTrackOutput = new FakeTrackOutput(false);
    avcChunkHandler = new AvcChunkHandler(0, fakeTrackOutput,
        new ChunkClock(10_000_000L, 24 * 10), FORMAT_BUILDER_AVC);
  }

  private void peekStreamHeader() throws IOException {
    final Context context = ApplicationProvider.getApplicationContext();
    final byte[] bytes =
        TestUtil.getByteArray(context,"extractordumps/avi/avc_sei_sps_pps_ird.dump");

    final FakeExtractorInput input = new FakeExtractorInput.Builder().setData(bytes).build();

    avcChunkHandler.peek(input, bytes.length);
  }

  @Test
  public void peek_givenStreamHeader() throws IOException {
    peekStreamHeader();
    final PicCountClock picCountClock = avcChunkHandler.getPicCountClock();
    Assert.assertNotNull(picCountClock);
    Assert.assertEquals(64, picCountClock.getMaxPicCount());
    Assert.assertEquals(0, avcChunkHandler.getSpsData().picOrderCountType);
    Assert.assertEquals(1.18f, fakeTrackOutput.lastFormat.pixelWidthHeightRatio, 0.01f);
  }

  @Test
  public void newChunk_givenStreamHeaderAndPSlice() throws IOException {
    peekStreamHeader();
    final PicCountClock picCountClock = avcChunkHandler.getPicCountClock();
    final FakeExtractorInput input = new FakeExtractorInput.Builder().setData(P_SLICE).build();

    avcChunkHandler.newChunk(P_SLICE.length, input);

    Assert.assertEquals(12, picCountClock.getLastPicCount());
  }
}
