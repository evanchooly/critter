package com.antwerkz.critter.test

import xyz.morphia.annotations.Embedded
import xyz.morphia.annotations.Property

@Embedded
class Address {
    @Property("c")
    var city: String? = null
    var state: String? = null
    var zip: String? = null

    constructor() {}

    constructor(city: String, state: String, zip: String) {
        this.city = city
        this.state = state
        this.zip = zip
    }

    override fun toString(): String {
        return "Address{" +
                "city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", zip='" + zip + '\'' +
                '}'
    }
}
