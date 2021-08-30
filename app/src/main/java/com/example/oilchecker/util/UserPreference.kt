package com.example.oilchecker.util

class UserPreference {
    companion object {

        var currentDevice: String = ""
        fun getDevice(): String? {
            val current = SpUtils.getString("current")
            return current
        }
        fun setDevice(num: String) {
            SpUtils.put("current",num)
        }

        fun getMac(): String? {
            return SpUtils.getString("mac")
        }
        fun setMac(mac: String) {
            SpUtils.put("mac",mac)
        }
        fun getIdentify(): String? {
            val value = SpUtils.getString("identify")
            return if (value!!.isEmpty()){
                "0000"
            }else{
                value!!
            }
        }
        fun setIdentify(identify: String) {
            SpUtils.put("identify",identify)
        }
        fun getAverageOil(): String? {
            return SpUtils.getString("average")
        }
        fun setAverageOil(average: String) {
            SpUtils.put("average",average)
        }
        fun getStatus(): String? {
            return SpUtils.getString("status")
        }

        fun setThresholdValue(value: Double){
            SpUtils.put("threshold", value)
        }

        fun getThreshold():Double{
            val value = SpUtils.getDouble("threshold")
            return if (value == 0.0){
                5.00
            }else{
                value!!
            }
        }

        //首页segment index
        fun setSegmentIndex(value: Int){
            SpUtils.put("segmentIndex",value)
        }

        fun getSegmentIndex(): Int{
            val value = SpUtils.getInt("segmentIndex")
            return if (value == -1){
                2
            }else{
                value!!
            }
        }

        fun setDateOffset(value: Int){
            SpUtils.put("DateOffset", value)
        }

        fun getDateOffset(): Int{
            val value = SpUtils.getInt("DateOffset")
            return if (value == -1){
                0
            }else{
                value!!
            }
        }
    }
}