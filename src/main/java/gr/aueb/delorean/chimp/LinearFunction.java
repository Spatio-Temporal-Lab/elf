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

import java.util.Objects;

public class LinearFunction {

    /** Constructors **/
    public LinearFunction(long ts, double vs, long te, double ve) {
        this.a = (ve - vs) / (te - ts);
        this.b = vs - a * ts;
    }

    public LinearFunction(double a, double b) {
        this.a = a;
        this.b = b;
    }

    /** Public Methods **/
    public double get(long ts) {
        return (this.a * ts + this.b);
    }

    /** Instance Variables **/
    public final double a, b;

    @Override
    public String toString() {
    	return String.format("%.15fx+%f", a, b);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LinearFunction other = (LinearFunction) obj;
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(other.a)
                && Double.doubleToLongBits(b) == Double.doubleToLongBits(other.b);
    }


}
