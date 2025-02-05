/**
 * Copyright 2021 Dynatrace LLC
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dynatrace.metric.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dynatrace.testutils.TestUtils;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class NormalizeTest {

  @ParameterizedTest(name = "{index}: {0}, input: {1}, expected: {2}")
  @MethodSource("provideDimensionKeys")
  public void testDimensionKey(String name, String input, String expected) {
    assertEquals(expected, Normalize.dimensionKey(input));
  }

  @ParameterizedTest(name = "{index}: {0}, input: {1}, expected: {2}")
  @MethodSource("provideDimensionValues")
  public void testDimensionValue(String name, String input, String expected) {
    assertEquals(expected, Normalize.dimensionValue(input));
  }

  @ParameterizedTest(name = "{index}: {0}, input: {1}, expected: {2}")
  @MethodSource("provideMetricKeys")
  public void testMetricKey(String name, String input, String expected) {
    assertEquals(expected, Normalize.metricKey(input));
  }

  @ParameterizedTest(name = "{index}: {0}, input: {1}, expected: {2}")
  @MethodSource("provideToEscapeValues")
  public void testEscapeDimensionValues(String name, String input, String expected) {
    assertEquals(expected, Normalize.escapeDimensionValue(input));
  }

  @ParameterizedTest(name = "{index}: {0}, input: {1}, expected: {2}")
  @MethodSource("provideNeedToEscapeDimensionValue")
  public void testNeedToEscapeDimensionValue(String name, String input, boolean expected) {
    assertEquals(expected, Normalize.needToEscapeDimensionValue(input));
  }

  @Test
  public void testDimensionValuesEscapedOnlyOnce() throws MetricException {
    MetricBuilderFactory metricBuilderFactory = MetricBuilderFactory.builder().build();
    String expected = "metric1,key=\\ \\,\\=\\\\ count,delta=123 1620392690261";

    String actual =
        metricBuilderFactory
            .newMetricBuilder("metric1")
            .setDimensions(DimensionList.create(Dimension.create("key", " ,=\\")))
            .setLongCounterValueDelta(123)
            .setTimestamp(Instant.ofEpochMilli(1620392690261L))
            .serializeMetricLine();

    assertEquals(expected, actual);
  }

  private static Stream<Arguments> provideMetricKeys() {
    return Stream.of(
        Arguments.of("valid base case", "basecase", "basecase"),
        Arguments.of("valid base case", "just.a.normal.key", "just.a.normal.key"),
        Arguments.of("valid leading underscore", "_case", "_case"),
        Arguments.of("valid underscore", "case_case", "case_case"),
        Arguments.of("valid number", "case1", "case1"),
        Arguments.of("invalid leading number", "1case", "_case"),
        Arguments.of("invalid multiple leading", "!@#case", "_case"),
        Arguments.of("invalid multiple trailing", "case!@#", "case_"),
        Arguments.of("valid leading uppercase", "Case", "Case"),
        Arguments.of("valid all uppercase", "CASE", "CASE"),
        Arguments.of("valid intermittent uppercase", "someCase", "someCase"),
        Arguments.of("valid multiple sections", "prefix.case", "prefix.case"),
        Arguments.of("valid multiple sections upper", "This.Is.Valid", "This.Is.Valid"),
        Arguments.of("invalid multiple sections leading number", "0a.b", "_a.b"),
        Arguments.of("valid multiple section leading underscore", "_a.b", "_a.b"),
        Arguments.of("valid leading number second section", "a.0", "a.0"),
        Arguments.of("valid leading number second section 2", "a.0.c", "a.0.c"),
        Arguments.of("valid leading number second section 3", "a.0b.c", "a.0b.c"),
        Arguments.of("invalid leading hyphen", "-dim", "_dim"),
        Arguments.of("valid trailing hyphen", "dim-", "dim-"),
        Arguments.of("valid trailing hyphens", "dim---", "dim---"),
        Arguments.of("invalid empty", "", null),
        Arguments.of("invalid only number", "000", "_"),
        Arguments.of("invalid key first section only number", "0.section", "_.section"),
        Arguments.of("invalid leading character", "~key", "_key"),
        Arguments.of("invalid leading characters", "~0#key", "_key"),
        Arguments.of("invalid intermittent character", "some~key", "some_key"),
        Arguments.of("invalid intermittent characters", "some#~äkey", "some_key"),
        Arguments.of("invalid two consecutive dots", "a..b", "a.b"),
        Arguments.of("invalid five consecutive dots", "a.....b", "a.b"),
        Arguments.of("invalid just a dot", ".", null),
        Arguments.of("invalid three dots", "...", null),
        Arguments.of("invalid leading dot", ".a", null),
        Arguments.of("invalid trailing dot", "a.", "a"),
        Arguments.of("invalid enclosing dots", ".a.", null),
        Arguments.of("valid consecutive leading underscores", "___a", "___a"),
        Arguments.of("valid consecutive trailing underscores", "a___", "a___"),
        Arguments.of("invalid trailing invalid chars groups", "a.b$%@.c#@", "a.b_.c_"),
        Arguments.of("valid consecutive enclosed underscores", "a___b", "a___b"),
        Arguments.of("invalid mixture dots underscores", "._._._a_._._.", null),
        Arguments.of("valid mixture dots underscores 2", "_._._.a_._", "_._._.a_._"),
        Arguments.of("invalid empty section", "an..empty.section", "an.empty.section"),
        Arguments.of("invalid characters", "a,,,b  c=d\\e\\ =,f", "a_b_c_d_e_f"),
        Arguments.of(
            "invalid characters long",
            "a!b\"c#d$e%f&g'h(i)j*k+l,m-n.o/p:q;r<s=t>u?v@w[x]y\\z^0 1_2;3{4|5}6~7",
            "a_b_c_d_e_f_g_h_i_j_k_l_m-n.o_p_q_r_s_t_u_v_w_x_y_z_0_1_2_3_4_5_6_7"),
        Arguments.of("invalid trailing characters", "a.b.+", "a.b._"),
        Arguments.of("valid combined test", "metric.key-number-1.001", "metric.key-number-1.001"),
        Arguments.of("valid example 1", "MyMetric", "MyMetric"),
        Arguments.of("invalid example 1", "0MyMetric", "_MyMetric"),
        Arguments.of("invalid example 2", "mÄtric", "m_tric"),
        Arguments.of("invalid example 3", "metriÄ", "metri_"),
        Arguments.of("invalid example 4", "Ätric", "_tric"),
        Arguments.of("invalid example 5", "meträääääÖÖÖc", "metr_c"),
        Arguments.of(
            "invalid truncate key too long",
            TestUtils.repeatStringNTimes("a", 270),
            TestUtils.repeatStringNTimes("a", 250)));
  }

  private static Stream<Arguments> provideDimensionKeys() {
    return Stream.of(
        Arguments.of("valid case", "dim", "dim"),
        Arguments.of("valid number", "dim1", "dim1"),
        Arguments.of("valid leading underscore", "_dim", "_dim"),
        Arguments.of("invalid leading uppercase", "Dim", "dim"),
        Arguments.of("invalid internal uppercase", "dIm", "dim"),
        Arguments.of("invalid trailing uppercase", "diM", "dim"),
        Arguments.of("invalid leading umlaut and uppercase", "äABC", "_abc"),
        Arguments.of("invalid multiple leading", "!@#case", "_case"),
        Arguments.of("invalid multiple trailing", "case!@#", "case_"),
        Arguments.of("invalid all uppercase", "DIM", "dim"),
        Arguments.of("valid dimension colon", "dim:dim", "dim:dim"),
        Arguments.of("valid dimension underscore", "dim_dim", "dim_dim"),
        Arguments.of("valid dimension hyphen", "dim-dim", "dim-dim"),
        Arguments.of("invalid leading hyphen", "-dim", "_dim"),
        Arguments.of("valid trailing hyphen", "dim-", "dim-"),
        Arguments.of("valid trailing hyphens", "dim---", "dim---"),
        Arguments.of("invalid leading multiple hyphens", "---dim", "_dim"),
        Arguments.of("invalid leading colon", ":dim", "_dim"),
        Arguments.of("invalid chars", "~@#ä", "_"),
        Arguments.of("invalid trailing chars", "aaa~@#ä", "aaa_"),
        Arguments.of("valid trailing underscores", "aaa___", "aaa___"),
        Arguments.of("invalid only numbers", "000", "_"),
        Arguments.of("valid compound key", "dim1.value1", "dim1.value1"),
        Arguments.of("invalid compound leading number", "dim.0dim", "dim._dim"),
        Arguments.of("invalid compound only number", "dim.000", "dim._"),
        Arguments.of("invalid compound leading invalid char", "dim.~val", "dim._val"),
        Arguments.of("invalid compound trailing invalid char", "dim.val~~", "dim.val_"),
        Arguments.of("invalid compound only invalid char", "dim.~~~", "dim._"),
        Arguments.of("valid compound leading underscore", "dim._val", "dim._val"),
        Arguments.of("valid compound only underscore", "dim.___", "dim.___"),
        Arguments.of("valid compound long", "dim.dim.dim.dim", "dim.dim.dim.dim"),
        Arguments.of("invalid two dots", "a..b", "a.b"),
        Arguments.of("invalid five dots", "a.....b", "a.b"),
        Arguments.of("invalid leading dot", ".a", "a"),
        Arguments.of("valid colon in compound", "a.b:c.d", "a.b:c.d"),
        Arguments.of("invalid trailing dot", "a.", "a"),
        Arguments.of("invalid just a dot", ".", ""),
        Arguments.of("invalid trailing dots", "a...", "a"),
        Arguments.of("invalid enclosing dots", ".a.", "a"),
        Arguments.of("invalid leading whitespace", "   a", "_a"),
        Arguments.of("invalid trailing whitespace", "a   ", "a_"),
        Arguments.of("invalid internal whitespace", "a b", "a_b"),
        Arguments.of("invalid internal whitespace", "a    b", "a_b"),
        Arguments.of("invalid empty", "", ""),
        Arguments.of("valid combined key", "dim.val:count.val001", "dim.val:count.val001"),
        Arguments.of("invalid characters", "a,,,b  c=d\\e\\ =,f", "a_b_c_d_e_f"),
        Arguments.of(
            "invalid characters long",
            "a!b\"c#d$e%f&g'h(i)j*k+l,m-n.o/p:q;r<s=t>u?v@w[x]y\\z^0 1_2;3{4|5}6~7",
            "a_b_c_d_e_f_g_h_i_j_k_l_m-n.o_p:q_r_s_t_u_v_w_x_y_z_0_1_2_3_4_5_6_7"),
        Arguments.of("invalid example 1", "Tag", "tag"),
        Arguments.of("invalid example 2", "0Tag", "_tag"),
        Arguments.of("invalid example 3", "tÄg", "t_g"),
        Arguments.of("invalid example 4", "mytäääg", "myt_g"),
        Arguments.of("invalid example 5", "ääätag", "_tag"),
        Arguments.of("invalid example 6", "ä_ätag", "___tag"),
        Arguments.of("invalid example 7", "Bla___", "bla___"),
        Arguments.of(
            "invalid truncate key too long",
            TestUtils.repeatStringNTimes("a", 120),
            TestUtils.repeatStringNTimes("a", 100)));
  }

  private static Stream<Arguments> provideDimensionValues() {
    return Stream.of(
        Arguments.of("valid value", "value", "value"),
        Arguments.of("valid empty", "", ""),
        Arguments.of("pass null", null, ""),
        Arguments.of("valid uppercase", "VALUE", "VALUE"),
        Arguments.of("valid colon", "a:3", "a:3"),
        Arguments.of("valid value 2", "~@#ä", "~@#ä"),
        Arguments.of("valid spaces", "a b", "a b"),
        Arguments.of("valid comma", "a,b", "a,b"),
        Arguments.of("valid equals", "a=b", "a=b"),
        Arguments.of("valid backslash", "a\\b", "a\\b"),
        Arguments.of("valid multiple special chars", " ,=\\", " ,=\\"),
        Arguments.of("valid key-value pair", "key=\"value\"", "key=\"value\""),
        //     \u0000 NUL character, \u0007 bell character
        Arguments.of("invalid unicode", "\u0000a\u0007", "_a_"),
        Arguments.of("invalid unicode space", "a\u0001b", "a_b"),
        //     'Ab' in unicode:
        Arguments.of("valid unicode", "\u0034\u0066", "\u0034\u0066"),
        //     A umlaut, a with ring, O umlaut, U umlaut, all valid.
        Arguments.of("valid unicode", "\u0132_\u0133_\u0150_\u0156", "\u0132_\u0133_\u0150_\u0156"),
        Arguments.of("invalid leading unicode NUL", "\u0000a", "_a"),
        Arguments.of("invalid only unicode", "\u0000\u0000", "_"),
        Arguments.of("invalid consecutive leading unicode", "\u0000\u0000\u0000a", "_a"),
        Arguments.of("invalid consecutive trailing unicode", "a\u0000\u0000\u0000", "a_"),
        Arguments.of("invalid trailing unicode NUL", "a\u0000", "a_"),
        Arguments.of("invalid enclosed unicode NUL", "a\u0000b", "a_b"),
        Arguments.of("invalid consecutive enclosed unicode NUL", "a\u0000\u0007\u0000b", "a_b"),
        Arguments.of(
            "invalid truncate value too long",
            TestUtils.repeatStringNTimes("a", 270),
            TestUtils.repeatStringNTimes("a", 250)));
  }

  private static Stream<Arguments> provideToEscapeValues() {
    return Stream.of(
        Arguments.of("unescaped", "ab", "ab"),
        Arguments.of("escape spaces", "a b", "a\\ b"),
        Arguments.of("escape comma", "a,b", "a\\,b"),
        Arguments.of("escape equals", "a=b", "a\\=b"),
        Arguments.of("escape backslash", "a\\b", "a\\\\b"),
        Arguments.of("escape multiple special chars", "\" ,=\\", "\\\"\\ \\,\\=\\\\"),
        Arguments.of(
            "escape consecutive special chars", "  ,,==\\\\", "\\ \\ \\,\\,\\=\\=\\\\\\\\"),
        Arguments.of("escape key-value pair", "key=\"value\"", "key\\=\\\"value\\\""),
        Arguments.of(
            "escape too long string",
            TestUtils.repeatStringNTimes("=", 250),
            TestUtils.repeatStringNTimes("\\=", 125)),
        Arguments.of(
            "escape sequence not broken apart 1",
            TestUtils.repeatStringNTimes("a", 249) + "=",
            TestUtils.repeatStringNTimes("a", 249)),
        Arguments.of(
            "escape sequence not broken apart 2",
            TestUtils.repeatStringNTimes("a", 248) + "==",
            TestUtils.repeatStringNTimes("a", 248) + "\\="),
        Arguments.of(
            "escape sequence not broken apart 3",
            // 3 trailing backslashes before escaping
            TestUtils.repeatStringNTimes("a", 247) + "\\\\\\",
            // 1 escaped trailing backslash
            TestUtils.repeatStringNTimes("a", 247) + "\\\\"),
        Arguments.of(
            "dimension value of only backslashes",
            TestUtils.repeatStringNTimes("\\", 260),
            TestUtils.repeatStringNTimes("\\\\", 125)),
        Arguments.of("Null must not fail", null, null),
        Arguments.of("Empty must not fail", "", ""));
  }

  private static Stream<Arguments> provideNeedToEscapeDimensionValue() {
    return Stream.of(
        Arguments.of("quote", "a\"b", true),
        Arguments.of("space", "a b", true),
        Arguments.of("comma", "a,b", true),
        Arguments.of("equals", "a=b", true),
        Arguments.of("backslash", "a\\b", true),
        Arguments.of("need to escape multiple", "a\"b c,d=e\\f", true),
        Arguments.of(
            "no need to escape: a-Z",
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ",
            false),
        Arguments.of("no need to escape: other", "_-", false));
  }
}
