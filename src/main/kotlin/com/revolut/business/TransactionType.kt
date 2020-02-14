package com.revolut.business

enum class TransactionType(val sqlType:String){
    DEPOSIT("deposit"),
    WITHDRAWAL("withdrawal"),
    TRANSFER("transfer")
}