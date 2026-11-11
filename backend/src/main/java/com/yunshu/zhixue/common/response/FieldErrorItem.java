package com.yunshu.zhixue.common.response;

/**
 * 字段级校验错误。
 *
 * @param field  出错的请求字段名
 * @param reason 面向用户的错误原因
 */
public record FieldErrorItem(String field, String reason) {
}
