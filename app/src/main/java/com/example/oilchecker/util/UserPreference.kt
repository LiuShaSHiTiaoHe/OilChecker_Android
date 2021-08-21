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
            return SpUtils.getString("identify")
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
            if (value == 0.0){
                return  5.00
            }else{
                return  value!!
            }
        }

        //首页segment index
        fun setSegmentIndex(value: Int){
            SpUtils.put("segmentIndex",value)
        }

        fun getSegmentIndex(): Int{
            val value = SpUtils.getInt("segmentIndex")
            if (value == -1){
                return  2
            }else{
                return  value!!
            }
        }

        fun setDateOffset(value: Int){
            SpUtils.put("DateOffset", value)
        }

        fun getDateOffset(): Int{
            val value = SpUtils.getInt("DateOffset")
            if (value == -1){
                return  0
            }else{
                return  value!!
            }
        }
    }
}