/*******************************************************************************
 * Copyright 2013 Yongwoon Lee, Yungho Yu
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.github.bibreen.mecab_ko_lucene_analyzer;

import static org.junit.Assert.*;

import java.io.StringReader;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.*;
import org.junit.Test;

public class MeCabKoStandardTokenizerTest {
  private String tokenizerToString(Tokenizer tokenizer) throws Exception {
    String result = new String();
    OffsetAttribute extOffset = tokenizer.addAttribute(OffsetAttribute.class);
    PositionIncrementAttribute posIncrAtt = 
        tokenizer.addAttribute(PositionIncrementAttribute.class);
    CharTermAttribute term =
        (CharTermAttribute)tokenizer.addAttribute(CharTermAttribute.class);
    TypeAttribute type =
        (TypeAttribute)tokenizer.addAttribute(TypeAttribute.class);

    while (tokenizer.incrementToken() == true) {
      result += new String(term.buffer(), 0, term.length()) + ":";
      result += type.type() + ":";
      result += String.valueOf(posIncrAtt.getPositionIncrement()) + ":";
      result += String.valueOf(extOffset.startOffset()) + ":";
      result += String.valueOf(extOffset.endOffset());
      result += ",";
    }
    tokenizer.end();
    return result;
  }
  
  private Tokenizer createTokenizer(
      StringReader reader, int decompoundMinLength) {
    return new MeCabKoTokenizer(
        reader,
        "/usr/local/lib/mecab/dic/mecab-ko-dic",
        new StandardPosAppender(),
        decompoundMinLength);
  }
  
  @Test
  public void test() throws Exception {
    Tokenizer tokenizer = createTokenizer(
        new StringReader("꽃배달 꽃망울 오토바이"), 2);
    assertEquals(
        "꽃:N:1:0:1,배달:N:1:1:3,꽃망울:COMPOUND:1:4:7,망울:N:0:5:7," +
        "오토바이:N:1:8:12,",
        tokenizerToString(tokenizer));
   
    tokenizer.reset();
    tokenizer.setReader(new StringReader("소설 무궁화꽃이 피었습니다."));
    assertEquals(
        "소설:N:1:0:2,무궁화:COMPOUND:1:3:6,무궁:N:0:3:5,꽃이:EOJEOL:1:6:8," +
        "꽃:N:0:6:7,피었습니다:EOJEOL:1:9:14,",
        tokenizerToString(tokenizer));
    tokenizer.close();
  }
  
  @Test
  public void test1() throws Exception {
    Tokenizer tokenizer = createTokenizer(
        new StringReader("한국을 최대한 배려했다는 사실을 이해해주길 바란다."),
        TokenGenerator.DEFAULT_DECOMPOUND);
    assertEquals(
        "한국을:EOJEOL:1:0:3,한국:N:0:0:2,최대한:COMPOUND:1:4:7,최대:N:0:4:6," +
        "배려했다는:EOJEOL:1:8:13,배려:N:0:8:10,사실을:EOJEOL:1:14:17," +
        "사실:N:0:14:16,이해해주길:EOJEOL:1:18:23,이해:N:0:18:20," +
        "바란다:INFLECT:1:24:27,",
        tokenizerToString(tokenizer));
    tokenizer.close();
  }
  
  @Test
  public void testEmptyQuery() throws Exception {
    Tokenizer tokenizer = createTokenizer(
        new StringReader(""), TokenGenerator.DEFAULT_DECOMPOUND);
    assertEquals(false, tokenizer.incrementToken());
    tokenizer.close();
  }

  @Test
  public void testEmptyMorphemes() throws Exception {
    Tokenizer tokenizer = createTokenizer(
        new StringReader("!@#$%^&*"), TokenGenerator.DEFAULT_DECOMPOUND);
    assertEquals(false, tokenizer.incrementToken());
    tokenizer.close();
  }

  @Test
  public void testHanEnglish() throws Exception {
    Tokenizer tokenizer = createTokenizer(
        new StringReader("한win"), TokenGenerator.DEFAULT_DECOMPOUND);
    assertEquals("한:N:1:0:1,win:SL:1:1:4,", tokenizerToString(tokenizer));
    tokenizer.close();
  }
  
  @Test
  public void testCompound() throws Exception {
    Tokenizer tokenizer = createTokenizer(
        new StringReader("형태소"), TokenGenerator.DEFAULT_DECOMPOUND);
    assertEquals(
        "형태소:COMPOUND:1:0:3,형태:N:0:0:2,", tokenizerToString(tokenizer));
    tokenizer.close();
    
    tokenizer = createTokenizer(
        new StringReader("가고문헌"), TokenGenerator.DEFAULT_DECOMPOUND);
    assertEquals(
        "가고:N:1:0:2,가고문헌:COMPOUND:1:0:4,문헌:N:0:2:4,",
        tokenizerToString(tokenizer));
    tokenizer.close();
  }
}