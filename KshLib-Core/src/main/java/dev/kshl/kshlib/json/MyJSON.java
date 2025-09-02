package dev.kshl.kshlib.json;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

public class MyJSON extends JSONObject {
    public MyJSON() {
        super();
    }

    public MyJSON(String string) {
        super(string);
    }

    public Optional<Long> getLongOpt(String key) {
        return Optional.ofNullable(super.optLongObject(key, null));
    }

    public Optional<Integer> getIntegerOpt(String key) {
        return Optional.ofNullable(super.optIntegerObject(key, null));
    }

    public Optional<String> getStringOpt(String key) {
        return Optional.ofNullable(super.optString(key, null));
    }

    public Optional<Double> getDoubleOpt(String key) {
        return Optional.ofNullable(super.optDoubleObject(key, null));
    }

    public Optional<Boolean> getBooleanOpt(String key) {
        return Optional.ofNullable(super.optBooleanObject(key, null));
    }

    public Optional<Float> getFloatOpt(String key) {
        return Optional.ofNullable(super.optFloatObject(key, null));
    }

    public Optional<JSONArray> getJSONArrayOpt(String key) {
        return Optional.ofNullable(super.optJSONArray(key, null));
    }

    public Optional<JSONObject> getJSONObjectOpt(String key) {
        return Optional.ofNullable(super.optJSONObject(key, null));
    }

    public Optional<BigInteger> getBigIntegerOpt(String key) {
        return Optional.ofNullable(super.optBigInteger(key, null));
    }

    public Optional<BigDecimal> getBigDecimalOpt(String key) {
        return Optional.ofNullable(super.optBigDecimal(key, null));
    }

    public Optional<Number> getNumberOpt(String key) {
        return Optional.ofNullable(super.optNumber(key, null));
    }

    public Optional<Object> getOpt(String key) {
        return Optional.ofNullable(super.opt(key));
    }
}
