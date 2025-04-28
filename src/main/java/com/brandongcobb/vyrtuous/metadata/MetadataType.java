/*  MetadataType.java The primary purpose of this interface is to
 *  be a contract for describing the type of a metadata value.
 *
 *  Copyright (C) 2024  github.com/brandongrahamcobb
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.brandongcobb.vyrtuous.metadata;
/**
 * A contract for describing the type of a metadata value
 *
 * @param <T> the Java Type of the metadata value
 */
public interface MetadataType<T> {
    /**
     *  @return the Class object representing T
     */
    Class<T> getType();
    /**
     *  @return an unsupported operation exception for MetadataCodec
     *
     */
//    default MetadataCodec<T> getCodec() {
  //      throw new UnsupportedOperationException("No codec available of this type.");
    //}
    /**
     *  @return an unsupported operation exception for MetadataRenderer
     *
     */
//    default MetadataRenderer<T> getRenderer() {
  //      throw new UnsupportedOperationException("No codec available of this type.");
    //}
}
