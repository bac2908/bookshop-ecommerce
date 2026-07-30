package vn.fs.backend.domain.model;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
		name = "favorites",
		uniqueConstraints = @UniqueConstraint(
				name = "ux_favorite_user_product",
				columnNames = {"user_id", "product_id"}))
public class Favorite implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long favoriteId;

	@ManyToOne(optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

}
