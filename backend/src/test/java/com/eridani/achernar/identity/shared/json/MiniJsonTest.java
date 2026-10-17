package com.eridani.achernar.identity.shared.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MiniJsonTest {

    @Test
    void escreveEReleObjetosAninhados() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("a", 1);
        m.put("b", "texto \"aspas\" \\ \n ç ü 漢");
        m.put("c", List.of(true, false, "x"));
        m.put("d", Map.of("k", 2.5));
        m.put("e", null);
        Map<String, Object> back = MiniJson.parseObject(MiniJson.write(m));
        assertThat(((Number) back.get("a")).intValue()).isEqualTo(1);
        assertThat(back.get("b")).isEqualTo("texto \"aspas\" \\ \n ç ü 漢");
        assertThat(back.get("c")).isEqualTo(List.of(true, false, "x"));
        assertThat(((Number) ((Map<?, ?>) back.get("d")).get("k")).doubleValue()).isEqualTo(2.5);
        assertThat(back.containsKey("e")).isTrue();
        assertThat(back.get("e")).isNull();
    }

    @Test
    void leUnicodeEscapado() {
        assertThat(MiniJson.parseObject("{\"x\":\"\\u00e7\\u00e3o\"}").get("x")).isEqualTo("ção");
    }

    @Test
    void rejeitaDocumentosInvalidos() {
        String[] ruins = {"", "[]", "{", "{\"a\":}", "{\"a\":1,}", "{\"a\":1} lixo", "{a:1}", "nulo"};
        for (String r : ruins) {
            assertThatThrownBy(() -> MiniJson.parseObject(r)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void escapaCaracteresDeControle() {
        String json = MiniJson.write(Map.of("k", "a\u0001b"));
        assertThat(json).contains("\\u0001");
    }

    @Test
    void obj_omiteNulosEMantemOrdem() {
        Obj o = Obj.of().with("z", 1).with("nulo", null).with("a", 2).withNullable("explicito", null);
        assertThat(o.keySet().stream().toList()).containsExactly("z", "a", "explicito");
    }
}
