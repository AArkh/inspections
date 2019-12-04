package com.plugin.inspection

class FILECLass(
    private val wannaBeString: Any?
) {

    @Throws(IllegalStateException::class)
    fun soFun() {
        if (wannaBeString is String) {
            println("yay!")
        } else {
            throw IllegalStateException("wannaBe is not a String =(")
        }
    }
}