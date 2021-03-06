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

import java.io.IOException;
import java.io.Reader;
import java.util.Queue;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.*;
import org.chasen.mecab.Lattice;
import org.chasen.mecab.Tagger;

/**
 * Lucene/Solr용 Tokenizer.
 * 
 * @author bibreen <bibreen@gmail.com>
 * @author amitabul <mousegood@gmail.com>
 */
public final class MeCabKoTokenizer extends Tokenizer {
  private CharTermAttribute charTermAtt;
  private PositionIncrementAttribute posIncrAtt;
  private OffsetAttribute offsetAtt;
  private TypeAttribute typeAtt;
 
  private String document;
  private String mecabDicDir;
  private MeCabLoader mecabLoader;
  private Lattice lattice;
  private Tagger tagger;
  private PosAppender posAppender;
  private int decompoundMinLength;
  private TokenGenerator generator;
  private Queue<TokenInfo> tokensQueue;

  /**
   * MeCabKoTokenizer 생성자.
   * 
   * @param input
   * @param dicDir mecab 사전 디렉터리 경로
   * @param appender PosAppender
   * @param decompoundMinLength 복합명사 분해를 하기위한 복합명사의 최소 길이.
   * 복합명사 분해가 필요없는 경우, TokenGenerator.NO_DECOMPOUND를 입력한다.
   */
  protected MeCabKoTokenizer(
      Reader input,
      String dicDir,
      PosAppender appender,
      int decompoundMinLength) {
    super(input);
    posAppender = appender;
    mecabDicDir = dicDir;
    this.decompoundMinLength = decompoundMinLength;
    setMeCab();
    setAttributes();
  }

  private void setMeCab() {
    mecabLoader = MeCabLoader.getInstance(mecabDicDir);
    lattice = mecabLoader.createLattice();
    tagger = mecabLoader.createTagger();
  }
  
  private void setAttributes() {
    charTermAtt = addAttribute(CharTermAttribute.class);
    posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    offsetAtt = addAttribute(OffsetAttribute.class);
    typeAtt = addAttribute(TypeAttribute.class);
  }

  @Override
  public boolean incrementToken() throws IOException {
    if (isBegin()) {
      document = getDocument();
      createTokenGenerator();
    }
    
    if (tokensQueue == null || tokensQueue.isEmpty()) {
      tokensQueue = generator.getNextEojeolTokens();
      if (tokensQueue == null) {
        return false;
      }
    }
    TokenInfo token = tokensQueue.poll();
    setAttributes(token);
    return true;
  }

  private boolean isBegin() {
    return generator == null;
  }

  private void createTokenGenerator() {
    lattice.set_sentence(document);
    tagger.parse(lattice);
    this.generator = new TokenGenerator(
        posAppender, decompoundMinLength, lattice.bos_node());
  }
  
  private void setAttributes(TokenInfo token) {
    posIncrAtt.setPositionIncrement(token.getPosIncr());
    offsetAtt.setOffset(
        correctOffset(token.getOffsets().start),
        correctOffset(token.getOffsets().end));
    charTermAtt.copyBuffer(
        token.getTerm().toCharArray(), 0, token.getTerm().length());
    typeAtt.setType(token.getPosTag());
  }
  
  @Override
  public final void end() {
    // set final offset
    offsetAtt.setOffset(
        correctOffset(document.length()), correctOffset(document.length()));
    document = null;
  }
  
  @Override
  public final void reset() throws IOException {
    super.reset();
    generator = null;
    tokensQueue = null;
  }
  
  private String getDocument() throws IOException {
    StringBuffer document = new StringBuffer();
    char[] tmp = new char[1024];
    int len;
    while ((len = input.read(tmp)) != -1) {
      document.append(new String(tmp, 0, len));
    }
    return document.toString().toLowerCase();
  }
}
