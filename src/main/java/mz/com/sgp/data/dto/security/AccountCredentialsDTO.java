package mz.com.sgp.data.dto.security;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class AccountCredentialsDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private String username;

	private String password;

	private String fullname;

	private String image;

	private List<String> roles;

	public AccountCredentialsDTO() {
	}

	public AccountCredentialsDTO(String username, String password, String fullname) {
		this.username = username;
		this.password = password;
		this.fullname = fullname;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getFullname() {
		return fullname;
	}

	public void setFullname(String fullname) {
		this.fullname = fullname;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public List<String> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

	@Override
	public int hashCode() {
		return Objects.hash(fullname, image, password, roles, username);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AccountCredentialsDTO other = (AccountCredentialsDTO) obj;
		return Objects.equals(fullname, other.fullname) && Objects.equals(image, other.image)
				&& Objects.equals(password, other.password) && Objects.equals(roles, other.roles)
				&& Objects.equals(username, other.username);
	}

}
