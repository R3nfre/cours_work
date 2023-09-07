package com.example.course_work.encryption;

import com.example.course_work.encryption.polynom.PolynomModQN;
import lombok.Getter;
import lombok.Setter;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Random;

import static com.example.course_work.encryption.polynom.PolynomModQN.add;
import static com.example.course_work.encryption.polynom.PolynomModQN.multiply;

public class NTRUEncrypt {
    private BigInteger p;
    private BigInteger q;
    private BigInteger e;
    PublicKey publicKey;
    PrivateKey privateKey;

    private PolynomModQN f, g, h;
    private PolynomModQN f_q, f_p;


    public static int constant_n = 251;
    public static int constant_p = 3;
    public static int constant_q = 128;

    public static int df = 50;
    public static int dg = 24;
    public static int dr = 16;

    public NTRUEncrypt() throws Exception {
        generate_key();
    }

    public NTRUEncrypt(PolynomModQN h) {
        this.h = h;
    }

    public PolynomModQN getH() {
        return h;
    }

    public PolynomModQN[] getKeyPair() {
        PolynomModQN[] pair = new PolynomModQN[2];
        pair[0] = f_q;
        pair[1] = f_p;
        return pair;
    }

    public int[] byte_to_coef(byte[] array_byte) {
        int[] array_int = new int[constant_n];

        for (int i = 0; i < array_byte.length * 8; i++)
            array_int[i] = (array_byte[i / 8] >> (i % 8)) & 1;
        array_int[array_byte.length * 8] = 1;

        return array_int;
    }

    public byte[] polynom_to_byte(PolynomModQN polynom) {
        int count_bit = Integer.toBinaryString(constant_q - 1).length();
        byte[] res = new byte[polynom._coef.length * count_bit / 8 + 1];

        for (int i = 0; i < polynom._coef.length * count_bit; i++) {
            byte x = (byte) (((polynom._coef[i / count_bit] >> (i % count_bit)) & 1) << (i % 8));
            res[i / 8] = (byte) (res[i / 8] | x);
        }
        return res;
    }

    public byte[] coef_to_byte(int[] array_int) {
        int index = array_int.length - 1;
        while (array_int[index] != 1) index--;

        if (index % 8 != 0) throw new IllegalArgumentException("Incorrect array");
        byte[] array_byte = new byte[index / 8];

        for (int i = 0; i < index; i++)
            array_byte[i / 8] = (byte) (array_byte[i / 8] | (array_int[i] << (i % 8)));

        return array_byte;
    }

    public PolynomModQN byte_to_polynom(byte[] array_byte) {
        int count_bit = Integer.toBinaryString(constant_q - 1).length();
        int[] array_int = new int[array_byte.length * 8 / count_bit];

        for (int i = 0; i < array_int.length * count_bit; i++) {
            int x = ((array_byte[i / 8] >> (i % 8)) & 1) << (i % count_bit);
            array_int[i / count_bit] = array_int[i / count_bit] | x;
        }
        return new PolynomModQN(array_int, constant_q, constant_n);
    }

    public byte[] encryption(byte[] array_byte) throws Exception {
        if (h == null) throw new Exception("No encryption key");

        PolynomModQN r = PolynomModQN.small_polynom(dr, dr);
        PolynomModQN m = new PolynomModQN(byte_to_coef(array_byte), constant_q, constant_n);
        PolynomModQN e = add(multiply(r, h), m);

        return polynom_to_byte(e);
    }

    public byte[] decryption(byte[] array_byte) throws Exception {
        if (f == null || f_p == null) throw new Exception("No decryption key");

        PolynomModQN e = byte_to_polynom(array_byte);
        PolynomModQN a = multiply(f, e);
        PolynomModQN new_a = new PolynomModQN(a.range_coef().coef, f_p._q, f_p._degree + 1);
        PolynomModQN m = multiply(f_p, new_a);

        return coef_to_byte(m.range_coef().coef);
    }

    public void generate_key() throws Exception {
        while (f_q == null || f_p == null) {
            f = PolynomModQN.small_polynom(df, df - 1);
            g = PolynomModQN.small_polynom(dg, dg);
            PolynomModQN f2 = new PolynomModQN(f.range_coef().coef, constant_p, constant_n);

            f_q = f.inverse();
            f_p = f2.inverse();
        }
        h = multiply(multiply(f_q, constant_p), g);
    }

    public static BigInteger generateCoprime(BigInteger num) {
        BigInteger start = new BigInteger(30, new Random());
        while (!start.gcd(num).equals(BigInteger.ONE) && start.compareTo(num) < 0) {
            System.out.println("num is not coprime: " + start);
            start = start.add(BigInteger.ONE);
        }
        if (!start.gcd(num).equals(BigInteger.ONE)) throw new RuntimeException("can't generate coprime number");
        return start;
    }

    public static int legandre(BigInteger a, BigInteger p) {
        if (a.equals(BigInteger.ONE)) return 1;

        // если число четное
        if (!a.testBit(0)) {
            BigInteger division = p.pow(2).subtract(BigInteger.ONE).divide(BigInteger.valueOf(8));
            int b = division.testBit(0) ? -1 : 1;
            return legandre(a.divide(BigInteger.TWO), p) * b;
        } else {
            BigInteger division = a.subtract(BigInteger.ONE).multiply(p.subtract(BigInteger.ONE)).divide(BigInteger.valueOf(4));
            return legandre(p.mod(a), a) * (division.testBit(0) ? -1 : 1);
        }

    }

    /**
     * @param key public key
     */
    public byte[] encrypt(byte[] message, byte[] key) {
        String[] keyStr = new String(key).split(":");
        System.out.println();
        BigInteger ed = getBigIntFromKey(keyStr[0]);
        // BigInteger N = getBigIntFromKey(keyStr[1]);
        BigInteger msg = getBigIntFromKey(new String(message));
        return _encrypt(message, key);
    }

    public byte[] decrypt(byte[] message, byte[] key) {
        return _decrypt(message, key);
    }

    public byte[] generatePublicKey() {
        p = BigInteger.probablePrime(32, new Random());
        q = BigInteger.probablePrime(32, new Random());
        BigInteger N = p.multiply(q);
        // generate e : gcd(e, (q-1)(p-1)(p+1)(q+1)) = 1
        BigInteger e = generateCoprime(p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE)).multiply(p.add(BigInteger.ONE)).multiply(q.add(BigInteger.ONE)));
        Key key = new Key(e, N, true);
        //return key.toString().getBytes();
        return publicKey.getEncoded();
    }

    public byte[] generatePrivateKey(byte[] C) {
        BigInteger msg = new BigInteger(C);
        int lgdP = legandre(msg.pow(2).subtract(BigInteger.valueOf(4)), p);
        int lgdQ = legandre(msg.pow(2).subtract(BigInteger.valueOf(4)), q);
        BigInteger d = null;
        if (lgdP == -1 && lgdQ == -1) d = lcm(p.add(BigInteger.ONE), q.add(BigInteger.ONE));
        else if (lgdP == 1 && lgdQ == -1) d = lcm(p.subtract(BigInteger.ONE), q.add(BigInteger.ONE));
        else if (lgdP == -1 && lgdQ == 1) d = lcm(p.add(BigInteger.ONE), q.subtract(BigInteger.ONE));
        else if (lgdP == 1 && lgdQ == 1) d = lcm(p.subtract(BigInteger.ONE), q.subtract(BigInteger.ONE));

        //return (d.toString() + ":" + p.multiply(q).toString()).getBytes();
        return getbytes(new Key(d, p.multiply(q), false), false);

    }

    private BigInteger getBigIntFromKey(String key) {
        return BigInteger.ONE;
    }

    private byte[] _encrypt(byte[] message, byte[] key) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(key);
            PublicKey pKey = keyFactory.generatePublic(publicKeySpec);
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, pKey);
            return cipher.doFinal(message);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeySpecException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            throw new RuntimeException(ex);
        }
    }

    private byte[] _decrypt(byte[] message, byte[] key) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            //EncodedKeySpec privateKeySpec = new X509EncodedKeySpec(key);
            //keyFactory.generatePrivate(privateKeySpec);
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(message);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            throw new RuntimeException(ex);
        }
    }


    private byte[] getbytes(Key key, boolean isPublic) {
        if (isPublic) return publicKey.getEncoded();
        else return privateKey.getEncoded();
    }

    BigInteger pow(BigInteger base, BigInteger exponent, BigInteger mod) {
        BigInteger result = BigInteger.ONE;
        while (exponent.signum() > 0) {
            if (exponent.testBit(0)) result = result.multiply(base).mod(mod);
            base = base.multiply(base).mod(mod);
            exponent = exponent.shiftRight(1);
        }
        return result;
    }

    public BigInteger genPrime(int bitLen) {
        BigInteger num = new BigInteger(bitLen, new Random());
        // проверяем на четность
        if (!num.testBit(0)) num = num.add(BigInteger.ONE);
        // идем от него и проверяеи на простоту
        while (!MillerRabinTest(num, 50)) {
            System.out.println("num is not prime: " + num);
            num = num.add(BigInteger.TWO);
        }
        return num;
    }

    public boolean MillerRabinTest(BigInteger n, int k) {
        // если n == 2 или n == 3 - эти числа простые, возвращаем true
        if (n.equals(BigInteger.TWO) || n.equals(BigInteger.valueOf(3))) return true;
        // если n < 2 или n четное - возвращаем false
        if (n.compareTo(BigInteger.TWO) < 0 || !n.testBit(0)) return false;
        // представим n − 1 в виде (2^s)·t, где t нечётно, это можно сделать последовательным делением n - 1 на 2
        BigInteger t = n.subtract(BigInteger.ONE);
        int s = 0;
        while (!t.testBit(0)) {
            t = t.divide(BigInteger.TWO);
            s += 1;
        }
        // повторить k раз
        for (int i = 0; i < k; i++) {
            // выберем случайное целое число a в отрезке [2, n − 2]
            int nLen = n.bitLength();
            BigInteger a = new BigInteger(nLen - 2, new Random());
            // x ← a^t mod n, вычислим с помощью возведения в степень по модулю
            BigInteger x = pow(a, t, n);
            // если x == 1 или x == n − 1, то перейти на следующую итерацию цикла
            if (x.equals(BigInteger.ONE) || x.equals(n.subtract(BigInteger.ONE))) continue;
            // повторить s − 1 раз
            for (int r = 1; r < s; r++) {
                // x ← x^2 mod n
                x = pow(x, BigInteger.TWO, n);
                // если x == 1, то вернуть "составное"
                if (x.equals(BigInteger.ONE)) return false;
                // если x == n − 1, то перейти на следующую итерацию внешнего цикла
                if (x.equals(n.subtract(BigInteger.ONE))) break;
            }
            if (!x.equals(n.subtract(BigInteger.ONE))) return false;
        }
        // вернуть "вероятно простое"
        return true;
    }

    private BigInteger lcm(BigInteger a, BigInteger b) {
        BigInteger gcd = a.gcd(b);
        return a.multiply(b).divide(gcd);
    }


    public static class Key {
        public Key(BigInteger ed, BigInteger N, boolean isPublic) {
            this.ed = ed;
            this.N = N;
            this.isPublic = isPublic;

        }

        public String toString() {
            return ed.toString() + ":" + N.toString();
        }

        @Setter
        @Getter
        private BigInteger ed;
        @Setter
        @Getter
        private BigInteger N;
        private boolean isPublic;
    }
}




