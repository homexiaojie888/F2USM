public class Stein {


        // Stein算法求最大公约数
        public static int gcd(int a, int b) {
            if (a == 0) return b;
            if (b == 0) return a;

            // a 和 b 都是偶数
            if ((a & 1) == 0 && (b & 1) == 0)
                return gcd(a >> 1, b >> 1) << 1;
                // a 是偶数，b 是奇数
            else if ((a & 1) == 0)
                return gcd(a >> 1, b);
                // a 是奇数，b 是偶数
            else if ((b & 1) == 0)
                return gcd(a, b >> 1);
                // a 和 b 都是奇数
            else if (a >= b)
                return gcd((a - b) >> 1, b);
            else
                return gcd(a, (b - a) >> 1);
        }

        // 求多个数的最大公约数
        public static int gcdOfMultipleNumbers(int[] numbers) {
            if (numbers == null || numbers.length == 0) {
                throw new IllegalArgumentException("输入数组为空");
            }

            int result = numbers[0];
            for (int i = 1; i < numbers.length; i++) {
                result = gcd(result, numbers[i]);
            }
            return result;
        }

        public static void main(String[] args) {
            int[] numbers = {24, 36, 48}; // 要求最大公约数的数字数组
            int result = gcdOfMultipleNumbers(numbers);
            System.out.println("最大公约数是：" + result);
        }


}
