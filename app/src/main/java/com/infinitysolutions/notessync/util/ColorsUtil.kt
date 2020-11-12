package com.infinitysolutions.notessync.util

class ColorsUtil {
    private val colorsList = arrayOf(
        "#2196f3",// 0
        "#AD1457",// 1
        "#FF5722",// 2
        "#9C27B0",// 3
        "#607D8B",// 4
        "#009688",// 5
        "#E91E63",// 6
        "#795548",// 7
        "#263238",//8
        "#3DDC84",//9
        "#00bcd4",//10
        "#f44336" //11
    )

    fun getColor(position: Int?): String {
        if (position == null || position > getSize())
            return colorsList[0]
        return colorsList[position]
    }

    fun getSize(): Int{
        return colorsList.size
    }
}