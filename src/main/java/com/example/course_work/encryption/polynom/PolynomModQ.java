package com.example.course_work.encryption.polynom;


import com.example.course_work.encryption.NTRUEncrypt;

import java.util.Arrays;

public class PolynomModQ {
    public int[] _coef;
    public int _degree;
    public int _q; //mod a

    public static class Polynom {
        public int[] coef;
        private int n;

        public Polynom(int[] a_i, int count) {
            n = count;
            coef = new int[n];
            System.arraycopy(a_i, 0, coef, 0, n);
        }
    }

    protected PolynomModQ() {
    }

    public PolynomModQ(int[] a_i, int mod) {
        int i = a_i.length - 1;
        while (a_i[i] % mod == 0 && i > 0)
            i--;

        _coef = new int[i + 1];
        _degree = i;
        _q = mod;

        for (i = 0; i < _coef.length; ++i)
            _coef[i] = (a_i[i] + mod) % mod;
    }

    public Polynom range_coef() {
        int[] new_polynom = new int[_coef.length];

        for (int i = 0; i < new_polynom.length; i++) {
            if (_coef[i] > _q / 2.0)
                new_polynom[i] = _coef[i] - _q;
            else
                new_polynom[i] = _coef[i];
        }
        return new Polynom(new_polynom, new_polynom.length);
    }

    public static PolynomModQ add(PolynomModQ polynom_1, PolynomModQ polynom_2) {
        if (polynom_1._q != polynom_2._q)
            throw new IllegalArgumentException("Unequal modul polynoms");

        int[] new_polynom = new int[Math.max(polynom_1._coef.length,
                polynom_2._coef.length)];

        if (polynom_1._degree > polynom_2._degree) {
            System.arraycopy(polynom_1._coef, 0, new_polynom, 0,
                    polynom_1._coef.length);

            for (int i = 0; i < polynom_2._coef.length; i++)
                new_polynom[i] += polynom_2._coef[i];
        } else {
            System.arraycopy(polynom_2._coef, 0, new_polynom, 0,
                    polynom_2._coef.length);

            for (int i = 0; i < polynom_1._coef.length; i++)
                new_polynom[i] += polynom_1._coef[i];
        }
        return new PolynomModQ(new_polynom, polynom_1._q);
    }

    public static PolynomModQ negate(PolynomModQ polynom) {
        int[] new_polynom = new int[polynom._coef.length];

        for (int i = 0; i < new_polynom.length; ++i)
            new_polynom[i] = -polynom._coef[i];

        return new PolynomModQ(new_polynom, polynom._q);
    }

    public static PolynomModQ subtract(PolynomModQ polynom_1, PolynomModQ
            polynom_2) {
        return add(polynom_1, negate(polynom_2));
    }

    public static PolynomModQ multiply(PolynomModQ polynom_1, PolynomModQ
            polynom_2) {
        if (polynom_1._q != polynom_2._q)
            throw new IllegalArgumentException("Unequal module polynoms");

        int[] new_polynom = new int[polynom_1._coef.length +
                polynom_2._coef.length];

        for (int i = 0; i < polynom_1._coef.length; i++)
            for (int j = 0; j < polynom_2._coef.length; j++)
                new_polynom[i + j] += polynom_1._coef[i] * polynom_2._coef[j];

        return new PolynomModQ(new_polynom, polynom_1._q);
    }

    public static boolean equals(PolynomModQ polynom_1, PolynomModQ polynom_2) {
        return Arrays.equals(polynom_1._coef, polynom_2._coef);
    }
}