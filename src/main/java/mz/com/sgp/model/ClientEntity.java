package mz.com.sgp.model;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import mz.com.sgp.config.audit.entity.AuditableEntity;

@Entity
@Table(name = "CLIENT")
public class ClientEntity extends AuditableEntity {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Column(name = "FIRST_NAME", nullable = false)
	private String firstName;

	@Column(name = "LAST_NAME", nullable = true)
	private String lastName;

	@Column(name = "PHONE_NUMBER", nullable = false)
	private String phoneNumber;

	@Column(name = "ADDRESS", nullable = true)
	private String address;

	@Column(name = "EMAIL", nullable = true)
	private String email;

	@Column(name = "TYPE", nullable = false)
	@Enumerated(EnumType.STRING)
	ClientType type;

	public ClientEntity(String firstName, String lastName, String phoneNumber, String address, String email,
			ClientType type) {
		super();
		this.firstName = firstName;
		this.lastName = lastName;
		this.phoneNumber = phoneNumber;
		this.address = address;
		this.email = email;
		this.type = type;
	}

	public ClientEntity() {
		super();
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public ClientType getType() {
		return type;
	}

	public void setType(ClientType type) {
		this.type = type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(address, email, firstName, lastName, phoneNumber, type);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClientEntity other = (ClientEntity) obj;
		return Objects.equals(address, other.address) && Objects.equals(email, other.email)
				&& Objects.equals(firstName, other.firstName) && Objects.equals(lastName, other.lastName)
				&& Objects.equals(phoneNumber, other.phoneNumber) && type == other.type;
	}

}
