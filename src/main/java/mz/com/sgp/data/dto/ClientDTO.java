package mz.com.sgp.data.dto;

import java.util.Objects;

import org.springframework.hateoas.server.core.Relation;

import mz.com.sgp.config.audit.dto.AuditableDTO;
import mz.com.sgp.model.ClientType;

@Relation(collectionRelation = "clients", itemRelation = "client")
public class ClientDTO extends AuditableDTO<ClientDTO> {

	private static final long serialVersionUID = 1L;

	private String firstName;

	private String lastName;

	private String phoneNumber;

	private String address;

	private String email;

	ClientType type;

	public ClientDTO() {
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
		ClientDTO other = (ClientDTO) obj;
		return Objects.equals(address, other.address) && Objects.equals(email, other.email)
				&& Objects.equals(firstName, other.firstName) && Objects.equals(lastName, other.lastName)
				&& Objects.equals(phoneNumber, other.phoneNumber) && type == other.type;
	}

}
