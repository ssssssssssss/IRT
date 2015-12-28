package tw.com.pmt.catsofun.core.db.model;

import java.util.Arrays;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.joda.time.LocalDateTime;

import tw.com.pmt.catsofun.core.common.entity.GenericEntity;

/**
 * 作答紀錄 Model
 *  
 * @author Billy
 *
 */
@Entity
@Table(name="record")
public class Record extends GenericEntity  {

	private static final long serialVersionUID = 3131964513654250815L;
	
	@Column(name = "init_ability", columnDefinition = "NUMERIC(8,5) default 0")
	private Double initAbility;
	
	@Column(name = "ability", columnDefinition = "NUMERIC(8,5) default 0")
	private Double ability;
	
	@Column(name = "selected_items")
	private Long[] selectedItems;
	
	@Column(name = "selected_options")
	private Long[] selectedOptions;
	
	@Column(name = "is_finished")
	private Boolean isFinished;

	@Column(name = "role_id")
	private Long roleId;
	
	@Column(name = "sem", columnDefinition = "NUMERIC(8,5) default 0")
	private Double sem;
	
	@Column(name="create_time")
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDateTime")
	private LocalDateTime createTime;
	
	@Column(name="test_complete_time")
	private Long testCompleteTime;
	
	public Double getInitAbility() {
		return initAbility;
	}

	public void setInitAbility(Double initAbility) {
		this.initAbility = initAbility;
	}

	public Double getAbility() {
		return ability;
	}

	public void setAbility(Double ability) {
		this.ability = ability;
	}

	public Long[] getSelectedItems() {
		return selectedItems;
	}

	public void setSelectedItems(Long[] selectedItems) {
		this.selectedItems = selectedItems;
	}

	public Boolean getIsFinished() {
		return isFinished;
	}

	public void setIsFinished(Boolean isFinished) {
		this.isFinished = isFinished;
	}

	public Long[] getSelectedOptions() {
		return selectedOptions;
	}

	public void setSelectedOptions(Long[] selectedOptions) {
		this.selectedOptions = selectedOptions;
	}

	public Long getRoleId() {
		return roleId;
	}

	public void setRoleId(Long roleId) {
		this.roleId = roleId;
	}

	public Double getSem() {
		return sem;
	}

	public void setSem(Double sem) {
		this.sem = sem;
	}

	public LocalDateTime getCreateTime() {
		return createTime;
	}

	public void setCreateTime(LocalDateTime createTime) {
		this.createTime = createTime;
	}

	public Long getTestCompleteTime() {
		return testCompleteTime;
	}

	public void setTestCompleteTime(Long testCompleteTime) {
		this.testCompleteTime = testCompleteTime;
	}

	@Override
	public String toString() {
		return "Record [initAbility=" + initAbility + ", ability=" + ability
				+ ", selectedItems=" + Arrays.toString(selectedItems)
				+ ", selectedOptions=" + Arrays.toString(selectedOptions)
				+ ", isFinished=" + isFinished + ", roleId=" + roleId
				+ ", sem=" + sem + ", createTime=" + createTime
				+ ", testCompleteTime=" + testCompleteTime + ", id=" + id + "]";
	}

}