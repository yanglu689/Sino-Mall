package com.sino.ware;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

// @SpringBootTest
class SinoMallWareApplicationTests {

    @Test
    void contextLoads() {
        int[] arr = new int[]{3, 2, 9, 8, 1, 8, 1, 8, 6, 10};
        final int length = arr.length;
        int num = 0;
        for (int i = 0; i < length - 1; i++) { // 控制外层次数
            num = 0;
            for (int j = 0; j < length - 1 - i; j++) { //控制比较项目 length -1 j+1后会比较到最后一个
                if (arr[j] < arr[j + 1]) {
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                }
                num++;
            }
            System.out.println(Arrays.toString(arr) + "：循环次数：" + num);
        }
        System.out.println("结果" + Arrays.toString(arr));

    }

}
