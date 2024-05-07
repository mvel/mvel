package org.mvel2.util;

import org.junit.Assert;
import org.junit.Test;
import org.mvel2.util.StringAppender;

public class StringAppenderTest {

  @Test
  public void testCharConstructor() {
    final StringAppender stringAppender = new StringAppender('a');
    Assert.assertEquals("", stringAppender.toString());
    // This looks to be exhibiting a potential bug, shouldn't this return "a"?
  }

  @Test
  public void testAppendNull() {
    final StringAppender stringAppender = new StringAppender("a");
    String s = null;
    stringAppender.append(s);
    Assert.assertEquals("a", stringAppender.toString());
  }

  @Test
  public void testAppendByteArr() {
    final StringAppender stringAppender = new StringAppender("a");
    stringAppender.append("b".getBytes());
    stringAppender.append("c".getBytes());
    Assert.assertEquals("abc", stringAppender.toString());
  }

  @Test
  public void testAppendByteArrSubSequence() {
    final StringAppender stringAppender = new StringAppender("a");
    stringAppender.append("bcd".getBytes(), 1, 2);
    stringAppender.append("e".getBytes(), 1, 0);
    Assert.assertEquals("acd", stringAppender.toString());
  }

  @Test
  public void testAppendCharArrSubSequence() {
    final StringAppender stringAppender = new StringAppender(new char[] { 'a' });
    stringAppender.append(new char[] { 'b', 'c', 'd' }, 1, 2);
    stringAppender.append(new char[] { 'e' }, 1, 0);
    Assert.assertEquals("acd", stringAppender.toString());
  }

  @Test
  public void testGetChars() {
    final StringAppender stringAppender = new StringAppender(new StringBuffer("abc"));
    Assert.assertArrayEquals(new char[] { 'a', 'b', 'c' }, stringAppender.getChars(0, 3));
  }

  @Test
  public void testGetCharsSubSequence() {
    final StringAppender stringAppender = new StringAppender(new StringBuffer("abcdef"));
    final char[] target = { 'g', 'h', 'i' };
    stringAppender.getChars(1, 2, target, 1);
    Assert.assertArrayEquals(new char[] { 'g', 'b', 'i' }, target);
  }

  @Test
  public void testCharAt() {
    final StringAppender stringAppender = new StringAppender("abc");
    Assert.assertEquals('b', stringAppender.charAt(1));
  }

  @Test
  public void testToCharsUnsupportedEncoding() {
    final StringAppender stringAppender = new StringAppender(0, "invalid");
    stringAppender.append("a".getBytes()[0]);
    final char[] expected = new char[15];
    expected[0] = 'a';
    Assert.assertArrayEquals(expected, stringAppender.toChars());
  }

  @Test
  public void testToString() {
    final StringAppender stringAppender = new StringAppender();
    stringAppender.append("a".getBytes()[0]);
    Assert.assertEquals("a", stringAppender.toString());
  }

  @Test
  public void testToStringUnsupportedEncoding() {
    final StringAppender stringAppender = new StringAppender(0, "invalid");
    stringAppender.append("a".getBytes()[0]);
    Assert.assertEquals("a", stringAppender.toString());
  }

  @Test
  public void testReset() {
    final StringAppender stringAppender = new StringAppender("abc");
    Assert.assertEquals(3, stringAppender.length());
    stringAppender.reset();
    Assert.assertEquals(0, stringAppender.length());
  }

  @Test
  public void testSubSequence() {
    final StringAppender stringAppender = new StringAppender("abcd");
    Assert.assertEquals("bc", stringAppender.subSequence(1, 3));
  }
}
