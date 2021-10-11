/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.time.ZonedDateTime;

import org.hibernate.type.descriptor.java.ZonedDateTimeJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.TimestampWithTimeZoneJdbcType;

/**
 * @author Steve Ebersole
 */
public class ZonedDateTimeType
		extends AbstractSingleColumnStandardBasicType<ZonedDateTime>  {

	/**
	 * Singleton access
	 */
	public static final ZonedDateTimeType INSTANCE = new ZonedDateTimeType();

	public ZonedDateTimeType() {
		super( TimestampWithTimeZoneJdbcType.INSTANCE, ZonedDateTimeJavaTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return ZonedDateTime.class.getSimpleName();
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}
}
