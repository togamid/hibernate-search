/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.valuebridge.param.string;

import java.time.Year;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.mapper.pojo.common.annotation.Param;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

// tag::include[]
@Entity
@Indexed
public class Book {

	@Id
	@GeneratedValue
	private Integer id;

	private String title;

	@GenericField(valueBinder = @ValueBinderRef(type = BooleanAsStringBinder.class, // <1>
			params = {
					@Param(name = "trueAsString", value = "yes"),
					@Param(name = "falseAsString", value = "no")
			}))
	private boolean published;

	@ElementCollection
	@GenericField(valueBinder = @ValueBinderRef(type = BooleanAsStringBinder.class, // <2>
			params = {
					@Param(name = "trueAsString", value = "passed"),
					@Param(name = "falseAsString", value = "failed")
			}), name = "censorshipAssessments_allYears")
	private Map<Year, Boolean> censorshipAssessments = new HashMap<>();

	// Getters and setters
	// ...

	// tag::getters-setters[]
	public Integer getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public boolean isPublished() {
		return published;
	}

	public void setPublished(boolean published) {
		this.published = published;
	}

	public Map<Year, Boolean> getCensorshipAssessments() {
		return censorshipAssessments;
	}

	public void setCensorshipAssessments(Map<Year, Boolean> censorshipAssessments) {
		this.censorshipAssessments = censorshipAssessments;
	}
// end::getters-setters[]
}
// end::include[]
