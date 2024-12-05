package cn.epbox.cv_npu;

/**
 * Created by tianzx on 24-8-19.
 */

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class UniqueNumbers {

    public static String removeDuplicates(String s) {
        // 拆分字符串为数组
        String[] numbers = s.split(",");

        // 使用 LinkedHashSet 去重并保持顺序
        Set<String> uniqueNumbers = new LinkedHashSet<>();
        for (String number : numbers) {
            uniqueNumbers.add(number);
        }

        // 将去重后的数字重新组合为字符串 API >26
        //String result_ = String.join(",", uniqueNumbers);
        StringBuilder result = new StringBuilder();
        for (String number : uniqueNumbers) {
            result.append(number);
        }
        // 检查 StringBuilder 是否非空
        if (!result.toString().isEmpty() && result.charAt(result.length() - 1) == ',') {
            // 删除最后一个字符
            result.deleteCharAt(result.length() - 1);
        }
        return result.toString();
    }

//    public static void main(String[] args) {
//        String input = "1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0";
//        String output = removeDuplicates(input);
//        System.out.println("Original: " + input);
//        System.out.println("Unique digits: " + output);
//    }
}