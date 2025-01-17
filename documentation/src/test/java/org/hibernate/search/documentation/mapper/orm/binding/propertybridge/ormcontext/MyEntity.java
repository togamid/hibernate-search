/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.propertybridge.ormcontext;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyBinding;

@Entity
@Indexed
public class MyEntity {

	@Id
	@GeneratedValue
	private Integer id;

	@PropertyBinding(binder = @PropertyBinderRef(type = MyDataPropertyBinder.class))
	private MyData myData;

	public Integer getId() {
		return id;
	}

	public MyData getMyData() {
		return myData;
	}

	public void setMyData(MyData myData) {
		this.myData = myData;
	}
}
