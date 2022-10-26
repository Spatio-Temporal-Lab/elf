/* Copyright 2018 The ModelarDB Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gr.aueb.delorean.chimp;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class LinearFunctionBigDecimal {

    MathContext mc = new MathContext(30, RoundingMode.HALF_UP) ;

    /** Constructors **/
    public LinearFunctionBigDecimal(long ts, BigDecimal vs, long te, BigDecimal ve) {
        this.a = ve.subtract(vs).divide(new BigDecimal(te - ts), mc);
        this.b = vs.subtract(a.multiply(new BigDecimal(ts)));
    }

    /** Public Methods **/
    public BigDecimal get(long ts) {
        return (this.a.multiply(new BigDecimal(ts)).add(b));
    }

    private BigDecimal a;
    /** Instance Variables **/
    private BigDecimal b;

    @Override
    public String toString() {
    	return String.format("%.15fx+%f", a, b);
    }

}
