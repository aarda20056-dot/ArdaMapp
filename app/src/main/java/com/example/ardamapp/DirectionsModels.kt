package com.example.ardamapp
data class DirectionsResponse(
    val routes: List<Route> = emptyList(),
    val status: String? = null,
    val error_message: String? = null
)

data class Route(
    val overview_polyline: OverviewPolyline? = null,
    val legs: List<Leg> = emptyList()
)

data class OverviewPolyline(
    val points: String? = null
)

data class Leg(
    val distance: TextValue? = null,
    val duration: TextValue? = null
)

data class TextValue(
    val text: String? = null,
    val value: Int? = null
)