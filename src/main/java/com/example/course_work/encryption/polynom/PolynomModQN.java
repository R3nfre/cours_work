package com.example.course_work.encryption.polynom;


import com.example.course_work.encryption.NTRUEncrypt;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class PolynomModQN extends PolynomModQ {
    private int _n;



    public PolynomModQN(int[] a_i, int mod, int count) {
        if (count < a_i.length) throw new IllegalArgumentException("Invalid degree of coefficients");

        _coef = new int[count];
        _n = count;
        _degree = count - 1;
        _q = mod;

        for (int i = 0; i < a_i.length; ++i)
            _coef[i] = (a_i[i] + mod) % mod;
    }

    public static PolynomModQN small_polynom(int count_1, int count_minus_1) {
        int[] new_coef = new int[NTRUEncrypt.constant_n];
        SecureRandom rand = new SecureRandom();

        for (int i = 0; i < NTRUEncrypt.constant_n; i++) {
            if (i < count_1) new_coef[i] = 1;
            else if (i < count_1 + count_minus_1) new_coef[i] = -1;
            else new_coef[i] = 0;
        }

        for (int i = NTRUEncrypt.constant_n - 1; i >= 1; i--) {
            int j = rand.nextInt(i + 1);

            int tmp = new_coef[j];
            new_coef[j] = new_coef[i];
            new_coef[i] = tmp;
        }
        return new PolynomModQN(new_coef, NTRUEncrypt.constant_q, NTRUEncrypt.constant_n);
    }

    public PolynomModQN inverse() throws Exception {
        final int range = 1000;
        int i = 0;
        List<PolynomModQ> quotients = new ArrayList<>();

        if (_q == NTRUEncrypt.constant_q) _q = 2;
        int[] x_n_coef = new int[_n + 1];
        x_n_coef[0] = -1;
        x_n_coef[_n] = 1;
        PolynomModQ x_n = new PolynomModQ(x_n_coef, _q);
        PolynomModQ balance = x_n;
        PolynomModQ f = new PolynomModQ(_coef, _q);

        int[] f_mod_coef = new int[1];
        PolynomModQ f_mod = new PolynomModQ(f_mod_coef, _q);

        int inv_n = inverse_int_mod(f._coef[f._coef.length - 1]);

        while (balance._degree >= f._degree && i < range) {
            int[] delta_n_coef = new int[balance._degree - f._degree + 1];
            delta_n_coef[delta_n_coef.length - 1] = balance._coef[balance._degree] * inv_n;
            PolynomModQ delta_n = new PolynomModQ(delta_n_coef, _q);

            f_mod = add(f_mod, delta_n);
            balance = subtract(balance, multiply(delta_n, f));
            i++;
        }
        quotients.add(f_mod);

        while (!equals(balance, new PolynomModQ(new int[]{0}, _q)) && i < range) {
            x_n = f;
            f = balance;
            f_mod = new PolynomModQ(new int[_n + 1], _q);
            balance = x_n;
            inv_n = inverse_int_mod(f._coef[f._coef.length - 1]);

            while (balance._degree >= f._degree && !equals(balance, new PolynomModQ(new int[]{0}, _q)) && i < range) {
                int[] delta_n_coef = new int[balance._degree - f._degree + 1];
                delta_n_coef[delta_n_coef.length - 1] = balance._coef[balance._degree] * inv_n;
                PolynomModQ delta_n = new PolynomModQ(delta_n_coef, _q);

                f_mod = add(f_mod, delta_n);
                balance = subtract(balance, multiply(delta_n, f));
                i++;
            }
            quotients.add(f_mod);
            i++;
        }
        //if (i > range)
        //  throw new Exception("Many iterations");

        List<PolynomModQ> x = new ArrayList<PolynomModQ>();
        x.add(new PolynomModQ(new int[]{0}, _q));
        x.add(new PolynomModQ(new int[]{1}, _q));

        for (int j = 0; j < quotients.size(); j++)
            x.add(add(multiply(quotients.get(j), x.get(j + 1)), x.get(j)));

        if (_q == 2) {
            int n = 2;
            _q = NTRUEncrypt.constant_q;
            PolynomModQN f_inverse = new PolynomModQN(x.get(x.size() - 2)._coef, _q, _n);
            while (n <= NTRUEncrypt.constant_q) {
                f_inverse = subtract(multiply(f_inverse, 2), multiply(multiply(this, f_inverse), f_inverse));
                n *= 2;
            }
            return f_inverse;
        }
        PolynomModQN f_inverse2 = new PolynomModQN(x.get(x.size() - 2)._coef, _q, _n);
        f_inverse2 = subtract(multiply(f_inverse2, _q), multiply(multiply(this, f_inverse2), f_inverse2));
        return multiply(f_inverse2, 2);
    }

    private int inverse_int_mod(int x) {
        for (int i = 1; i < _q; i++) {
            if ((x * i) % _q == 1) {
                return i;
            }
        }

        throw new ArithmeticException("No inverse element");
    }

    public static PolynomModQN add(PolynomModQN polynom_1, PolynomModQN polynom_2) {
        PolynomModQ res = add(polynom_1, (PolynomModQ) polynom_2);
        return new PolynomModQN(res._coef, polynom_1._q, polynom_1._n);
    }

    public static PolynomModQN subtract(PolynomModQN polynom_1, PolynomModQN polynom_2) {
        PolynomModQ res = subtract(polynom_1, (PolynomModQ) polynom_2);
        return new PolynomModQN(res._coef, polynom_1._q, polynom_1._n);
    }

    public static PolynomModQN multiply(PolynomModQN polynom_1, PolynomModQN polynom_2) {
        if (polynom_1._n != polynom_2._n) {
            throw new IllegalArgumentException("Invalid degree N");
        }

        int[] a_i = new int[polynom_1._n];
        for (int i = 0; i < polynom_1._n; ++i) {
            for (int j = 0; j < polynom_2._n; ++j) {
                a_i[i] += polynom_1._coef[j] * polynom_2._coef[(polynom_1._n + i - j) % polynom_1._n];
            }
        }

        return new PolynomModQN(a_i, polynom_1._q, polynom_1._n);
    }

    public static PolynomModQN multiply(PolynomModQN polynom, int x) {
        int[] a_i = new int[polynom._n];
        for (int i = 0; i < polynom._n; ++i) {
            a_i[i] = polynom._coef[i] * x;
        }
        return new PolynomModQN(a_i, polynom._q, polynom._n);
    }
}
