package com.revolut.business

enum class PostingType(val sqlType:String) {
    DEBIT("debit"),
    CREDIT("credit")
}