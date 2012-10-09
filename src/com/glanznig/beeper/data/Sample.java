package com.glanznig.beeper.data;

import java.util.Date;

public class Sample {
	
	private Long id;
	private Date timestamp;
	private String title;
	private String description;
	private Boolean accepted;
	
	public Long getId() {
		return id;
	}
	
	private void setId(Long id) {
		this.id = id;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Boolean getAccepted() {
		return accepted;
	}

	public void setAccepted(Boolean accepted) {
		this.accepted = accepted;
	}

}