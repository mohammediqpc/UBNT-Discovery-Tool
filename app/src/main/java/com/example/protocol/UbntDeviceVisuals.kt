package com.example.protocol

import com.example.R

object UbntDeviceVisuals {

    fun getDrawable(model: String, platform: String): Int {
        val s = "$model $platform".lowercase()

        return when {
            s.contains("nano") || s.contains("loco") || s.contains("ns5") || s.contains("ns2") || s.contains("nsm") -> {
                R.drawable.ic_device_nanostation
            }
            s.contains("litebeam") || s.contains("lbe") -> {
                R.drawable.ic_device_litebeam
            }
            s.contains("powerbeam") || s.contains("pbe") || s.contains("nanobeam") || s.contains("nbe") || s.contains("grid") || s.contains("ag5") -> {
                R.drawable.ic_device_powerbeam
            }
            s.contains("rocket") || s.contains("prism") || s.contains("r5ac") || s.contains("rm5") || s.contains("rm2") -> {
                R.drawable.ic_device_rocket
            }
            s.contains("bullet") || s.contains("picostation") || s.contains("groove") || s.contains("b2") || s.contains("b5") -> {
                R.drawable.ic_device_bullet
            }
            s.contains("isostation") || s.contains("prismstation") || s.contains("is-5ac") || s.contains("ps-5ac") -> {
                R.drawable.ic_device_isostation
            }
            s.contains("airfiber") || s.contains("af-") || s.contains("af5") || s.contains("af60") || s.contains("af24") || s.contains("af11") -> {
                R.drawable.ic_device_airfiber
            }
            s.contains("ltu") || s.contains("gigabeam") || s.contains("wave") || s.contains("gbe") -> {
                R.drawable.ic_device_ltu
            }
            s.contains("udm") || s.contains("gateway") || s.contains("uxg") || s.contains("cloud key") || s.contains("ucg") -> {
                R.drawable.ic_device_unifi_gateway
            }
            s.contains("unifi") || s.contains("uap") || s.contains("u6") || s.contains("u7") || s.contains("nanohd") || s.contains("ac-pro") || s.contains("ac-lr") || s.contains("ac-lite") || s.contains("flexhd") || s.contains("in-wall") -> {
                R.drawable.ic_device_unifi_ap
            }
            s.contains("edgerouter") || s.contains("er-") || s.contains("edgeswitch") || s.contains("es-") || s.contains("switch") || s.contains("usw") || s.contains("uisp-r") || s.contains("uisp-s") -> {
                R.drawable.ic_device_edgerouter
            }
            else -> {
                R.drawable.ic_device_generic_ubnt
            }
        }
    }
}
