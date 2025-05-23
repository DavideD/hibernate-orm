/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.query.Query;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.type.StandardBasicTypes;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;
import org.junit.runners.Parameterized;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-10372")
public class OffsetDateTimeTest extends AbstractJavaTimeTypeTest<OffsetDateTime, OffsetDateTimeTest.EntityWithOffsetDateTime> {

	@Override
	protected void configure(Configuration configuration) {
		super.configure(configuration);
		configuration.setProperty( AvailableSettings.TIMEZONE_DEFAULT_STORAGE, TimeZoneStorageType.NORMALIZE );
	}

	private static class ParametersBuilder extends AbstractParametersBuilder<ParametersBuilder> {
		public ParametersBuilder add(int year, int month, int day,
				int hour, int minute, int second, int nanosecond, String offset, ZoneId defaultTimeZone) {
			if ( !isNanosecondPrecisionSupported() ) {
				nanosecond = 0;
			}
			return add( defaultTimeZone, year, month, day, hour, minute, second, nanosecond, offset );
		}
	}

	@Parameterized.Parameters(name = "{1}-{2}-{3}T{4}:{5}:{6}.{7}[{8}] {0}")
	public static List<Object[]> data() {
		return new ParametersBuilder()
				// Not affected by any known bug
				.add( 2017, 11, 6, 19, 19, 1, 0, "+10:00", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "+07:00", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "+01:30", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "+01:00", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "+00:30", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "-02:00", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "-06:00", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "-08:00", ZONE_UTC_MINUS_8 )
				.add( 2017, 11, 6, 19, 19, 1, 0, "+10:00", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "+07:00", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "+01:30", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 500, "+01:00", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "+01:00", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "+00:30", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "-02:00", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "-06:00", ZONE_PARIS )
				.add( 2017, 11, 6, 19, 19, 1, 0, "-08:00", ZONE_PARIS )
				.skippedForDialects(
						// MySQL/Mariadb cannot store values equal to epoch exactly, or less, in a timestamp.
						Arrays.asList( MySQLDialect.class, MariaDBDialect.class ),
						b -> b
								// Not affected by any known bug
								.add( 1970, 1, 1, 0, 0, 0, 0, "+01:00", ZONE_GMT )
								.add( 1970, 1, 1, 0, 0, 0, 0, "+00:00", ZONE_GMT )
								.add( 1970, 1, 1, 0, 0, 0, 0, "-01:00", ZONE_GMT )
								.add( 1900, 1, 1, 0, 0, 0, 0, "+01:00", ZONE_GMT )
								.add( 1900, 1, 1, 0, 0, 0, 0, "+00:00", ZONE_GMT )
								.add( 1900, 1, 1, 0, 0, 0, 0, "-01:00", ZONE_GMT )
								.add( 1900, 1, 1, 0, 0, 0, 0, "+00:00", ZONE_OSLO )
								.add( 1900, 1, 1, 0, 9, 21, 0, "+00:09:21", ZONE_PARIS )
								.add( 1900, 1, 1, 0, 19, 32, 0, "+00:19:32", ZONE_PARIS )
								.add( 1900, 1, 1, 0, 19, 32, 0, "+00:19:32", ZONE_AMSTERDAM )
								.add( 1900, 1, 1, 0, 9, 20, 0, "+00:09:21", ZONE_PARIS )
								.add( 1900, 1, 1, 0, 19, 31, 0, "+00:19:32", ZONE_PARIS )
								.add( 1900, 1, 1, 0, 19, 31, 0, "+00:19:32", ZONE_AMSTERDAM )
				)
				.skippedForDialects(
						// MySQL/Mariadb cannot store values equal to epoch exactly, or less, in a timestamp.
						dialect -> dialect instanceof MySQLDialect || dialect instanceof MariaDBDialect
								|| dialect instanceof H2Dialect && ( (H2Dialect) dialect ).hasOddDstBehavior(),
						b -> b
								// Affected by HHH-13266 (JDK-8061577)
								.add( 1892, 1, 1, 0, 0, 0, 0, "+00:00", ZONE_OSLO )
				)
				.skippedForDialects(
						// MySQL/Mariadb/Sybase cannot store dates in 1600 in a timestamp.
						dialect -> dialect instanceof MySQLDialect || dialect instanceof MariaDBDialect || dialect instanceof SybaseDialect
							|| dialect instanceof H2Dialect && ( (H2Dialect) dialect ).hasOddDstBehavior(),
						b -> b
								.add( 1600, 1, 1, 0, 0, 0, 0, "+00:19:32", ZONE_AMSTERDAM )
				)
				// HHH-13379: DST end (where Timestamp becomes ambiguous, see JDK-4312621)
				// => This used to work correctly in 5.4.1.Final and earlier
				.skippedForDialects(
						dialect -> dialect instanceof H2Dialect && ( (H2Dialect) dialect ).hasOddDstBehavior(),
						b -> b.add( 2018, 10, 28, 2, 0, 0, 0, "+01:00", ZONE_PARIS )
								.add( 2018, 4, 1, 2, 0, 0, 0, "+12:00", ZONE_AUCKLAND )
				)
				// => This has never worked correctly, unless the JDBC timezone was set to UTC
				.withForcedJdbcTimezone( "UTC", b -> b
						.add( 2018, 10, 28, 2, 0, 0, 0, "+02:00", ZONE_PARIS )
						.add( 2018, 4, 1, 2, 0, 0, 0, "+13:00", ZONE_AUCKLAND )
				)
				// => Also test DST start, just in case
				.add( 2018, 3, 25, 2, 0, 0, 0, "+01:00", ZONE_PARIS )
				.add( 2018, 3, 25, 3, 0, 0, 0, "+02:00", ZONE_PARIS )
				.add( 2018, 9, 30, 2, 0, 0, 0, "+12:00", ZONE_AUCKLAND )
				.add( 2018, 9, 30, 3, 0, 0, 0, "+13:00", ZONE_AUCKLAND )
				// => Also test dates around 1905-01-01, because the code behaves differently before and after 1905
				.add( 1904, 12, 31, 23, 59, 59, 999_999_999, "-01:00", ZONE_PARIS )
				.add( 1904, 12, 31, 23, 59, 59, 999_999_999, "+00:00", ZONE_PARIS )
				.add( 1904, 12, 31, 23, 59, 59, 999_999_999, "+01:00", ZONE_PARIS )
				.add( 1905, 1, 1, 0, 0, 0, 0, "-01:00", ZONE_PARIS )
				.add( 1905, 1, 1, 0, 0, 0, 0, "+00:00", ZONE_PARIS )
				.add( 1905, 1, 1, 0, 0, 0, 0, "+01:00", ZONE_PARIS )
				.build();
	}

	private final int year;
	private final int month;
	private final int day;
	private final int hour;
	private final int minute;
	private final int second;
	private final int nanosecond;
	private final String offset;

	public OffsetDateTimeTest(EnvironmentParameters env, int year, int month, int day,
			int hour, int minute, int second, int nanosecond, String offset) {
		super( env );
		this.year = year;
		this.month = month;
		this.day = day;
		this.hour = hour;
		this.minute = minute;
		this.second = second;
		this.nanosecond = nanosecond;
		this.offset = offset;
	}

	@Override
	protected Class<EntityWithOffsetDateTime> getEntityType() {
		return EntityWithOffsetDateTime.class;
	}

	@Override
	protected EntityWithOffsetDateTime createEntityForHibernateWrite(int id) {
		return new EntityWithOffsetDateTime( id, getOriginalOffsetDateTime() );
	}

	private OffsetDateTime getOriginalOffsetDateTime() {
		return OffsetDateTime.of( year, month, day, hour, minute, second, nanosecond, ZoneOffset.of( offset ) );
	}

	@Override
	protected OffsetDateTime getExpectedPropertyValueAfterHibernateRead() {
		return getOriginalOffsetDateTime().atZoneSameInstant( ZoneId.systemDefault() ).toOffsetDateTime();
	}

	@Override
	protected OffsetDateTime getActualPropertyValue(EntityWithOffsetDateTime entity) {
		return entity.value;
	}

	@Override
	protected void setJdbcValueForNonHibernateWrite(PreparedStatement statement, int parameterIndex) throws SQLException {
		statement.setTimestamp( parameterIndex, getExpectedJdbcValueAfterHibernateWrite() );
	}

	@Override
	protected Timestamp getExpectedJdbcValueAfterHibernateWrite() {
		LocalDateTime dateTimeInDefaultTimeZone = getOriginalOffsetDateTime().atZoneSameInstant( ZoneId.systemDefault() )
				.toLocalDateTime();
		return new Timestamp(
				dateTimeInDefaultTimeZone.getYear() - 1900, dateTimeInDefaultTimeZone.getMonthValue() - 1,
				dateTimeInDefaultTimeZone.getDayOfMonth(),
				dateTimeInDefaultTimeZone.getHour(), dateTimeInDefaultTimeZone.getMinute(),
				dateTimeInDefaultTimeZone.getSecond(),
				dateTimeInDefaultTimeZone.getNano()
		);
	}

	@Override
	protected Object getActualJdbcValue(ResultSet resultSet, int columnIndex) throws SQLException {
		return resultSet.getTimestamp( columnIndex );
	}

	@Test
	public void testRetrievingEntityByOffsetDateTime() {
		withDefaultTimeZone( () -> {
			inTransaction( session -> {
				session.persist( new EntityWithOffsetDateTime( 1, getOriginalOffsetDateTime() ) );
			} );
			Consumer<OffsetDateTime> checkOneMatch = expected -> inSession( s -> {
				Query query = s.createQuery( "from " + ENTITY_NAME + " o where o.value = :date" );
				query.setParameter( "date", expected, StandardBasicTypes.OFFSET_DATE_TIME );
				List<EntityWithOffsetDateTime> list = query.list();
				assertThat( list.size(), is( 1 ) );
			} );
			checkOneMatch.accept( getOriginalOffsetDateTime() );
			checkOneMatch.accept( getExpectedPropertyValueAfterHibernateRead() );
			checkOneMatch.accept( getExpectedPropertyValueAfterHibernateRead().withOffsetSameInstant( ZoneOffset.UTC ) );
		} );
	}

	@Entity(name = ENTITY_NAME)
	static final class EntityWithOffsetDateTime {
		@Id
		@Column(name = ID_COLUMN_NAME)
		private Integer id;

		@Basic
		@Column(name = PROPERTY_COLUMN_NAME)
		private OffsetDateTime value;

		protected EntityWithOffsetDateTime() {
		}

		private EntityWithOffsetDateTime(int id, OffsetDateTime value) {
			this.id = id;
			this.value = value;
		}
	}
}
