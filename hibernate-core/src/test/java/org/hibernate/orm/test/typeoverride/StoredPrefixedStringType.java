/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.typeoverride;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.AssertionFailure;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.StringJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;

/**
 *
 * @author Gail Badner
 */
public class StoredPrefixedStringType
		extends AbstractSingleColumnStandardBasicType<String> {

	public static final String PREFIX = "PRE:";

	public static final JdbcType PREFIXED_VARCHAR_TYPE_DESCRIPTOR =
			new VarcharJdbcType() {
				public <X> ValueBinder<X> getBinder(final JavaType<X> javaTypeDescriptor) {
					return new BasicBinder<X>( javaTypeDescriptor, this ) {
						@Override
						protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
							String stringValue = javaTypeDescriptor.unwrap( value, String.class, options );
							st.setString( index, PREFIX + stringValue );
						}

						@Override
						protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
								throws SQLException {
							String stringValue = javaTypeDescriptor.unwrap( value, String.class, options );
							st.setString( name, PREFIX + stringValue );
						}
					};
				}

				public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaTypeDescriptor) {
					return new BasicExtractor<X>( javaTypeDescriptor, this ) {
						@Override
						protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
							String stringValue = rs.getString( paramIndex );
							if ( ! stringValue.startsWith( PREFIX ) ) {
								throw new AssertionFailure( "Value read from resultset does not have prefix." );
							}
							return javaTypeDescriptor.wrap( stringValue.substring( PREFIX.length() ), options );
						}

						@Override
						protected X doExtract(CallableStatement statement, int index, WrapperOptions options)
								throws SQLException {
							String stringValue = statement.getString( index );
							if ( ! stringValue.startsWith( PREFIX ) ) {
								throw new AssertionFailure( "Value read from procedure output param does not have prefix." );
							}
							return javaTypeDescriptor.wrap( stringValue.substring( PREFIX.length() ), options );
						}

						@Override
						protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
							String stringValue = statement.getString( name );
							if ( ! stringValue.startsWith( PREFIX ) ) {
								throw new AssertionFailure( "Value read from procedure output param does not have prefix." );
							}
							return javaTypeDescriptor.wrap( stringValue.substring( PREFIX.length() ), options );
						}
					};
				}
			};


	public static final StoredPrefixedStringType INSTANCE = new StoredPrefixedStringType();

	public StoredPrefixedStringType() {
		super( PREFIXED_VARCHAR_TYPE_DESCRIPTOR, StringJavaTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "string";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}
}
