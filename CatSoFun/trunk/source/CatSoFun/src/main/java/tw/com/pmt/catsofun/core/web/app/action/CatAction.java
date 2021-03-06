package tw.com.pmt.catsofun.core.web.app.action;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import tw.com.pmt.catsofun.core.business.service.IItemService;
import tw.com.pmt.catsofun.core.business.service.IRecordService;
import tw.com.pmt.catsofun.core.business.service.IResponseService;
import tw.com.pmt.catsofun.core.common.Parameter;
import tw.com.pmt.catsofun.core.common.util.ScopeUtil;
import tw.com.pmt.catsofun.core.common.util.ScopeUtil.Scope;
import tw.com.pmt.catsofun.core.db.model.Item;
import tw.com.pmt.catsofun.core.db.model.Record;
import tw.com.pmt.catsofun.core.db.model.Response;
import tw.com.pmt.catsofun.core.db.model.Role;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;

/**
 * 「測驗」控制器
 * 
 * @author Billy
 * 
 */
public class CatAction extends ActionSupport {

	private static final long serialVersionUID = 352281438045676179L;

	private String selectedOption;

	@Autowired
	private IItemService itemService;

	@Autowired
	private IResponseService responseService;

	@Autowired
	private IRecordService recordService;
	
	private Item item;
	private Response response;
	private Record record;
	
	private Boolean isFinished;
	private Double initAbility;
	private Double currentAbility;

	private static Double prior = Parameter.TEST_PRIOR_INVERSE_VARIANCE;
	private Long startTime;

	private List<Record> recordList;
	
	private String subjectCode;

	/**
	 * 初始化測驗開始時間
	 *  
	 * @return String
	 */
	public String initializeBeginTime() {
		//初始化時間 存入session
		startTime = new Date().getTime();
		Map<String, Object> sessionMap = ScopeUtil.getScopeAttribute(Scope.SESSION);
		sessionMap.put("startTime", startTime);

		return ActionSupport.SUCCESS;
	}
	
	/**
	 * 更新測驗完成狀態
	 *  
	 * @return String
	 */
	public String updateCaseRecord() {
		// Session取出初始化結果
		Map<String, Object> sessionMap = ScopeUtil.getScopeAttribute(Scope.SESSION);
		record = (Record) sessionMap.get("record");

		// 更新測驗完成狀態
		record.setIsFinished(false);

		recordService.updateRecord(record);
		
		return ActionSupport.SUCCESS;
	}
	
	/**
	 * 取得所有作答結果紀錄
	 *  
	 * @return String
	 */
	public String getHistoryRecord() {
		Role role = (Role) ActionContext.getContext().getSession().get("loginRole");

		if (role == null) {
			return ActionSupport.ERROR;
		}

		recordList = recordService.queryRecordByRoleName(role.getUserName());

		return ActionSupport.SUCCESS;
	}

	public String showCatMainPage() {
		System.out.println("showCatMainContent() begin...");

		String loginFlag = (String) ActionContext.getContext().getSession().get("loginFlag");

		System.out.println("loginFlag = " + loginFlag);

		// 確認是否已登入
		if ("success".equals(loginFlag)) {

			// 初始化可選題庫，選取題目
			isFinished = false;
			
			List<Long> selectedOptions = new ArrayList<Long>();
			List<Item> selectedItems = new ArrayList<Item>();;
			List<Item> chooseItemPool = new ArrayList<Item>();;
			chooseItemPool = itemService.getAllItem();
			
			initAbility = (Math.random() * 1 - 0.5);
			currentAbility = initAbility;

			// 正式選題
			item = itemSelection(initAbility, chooseItemPool, prior);
			selectedItems.add(item);

			for (int i = 0 ; i < chooseItemPool.size() ; i++) {
				if (item.getId() == chooseItemPool.get(i).getId()) {
					chooseItemPool.remove(i);
				}
			}

			// 取得作答反應 
			Long responseIndex = item.getAnswerType();
			response = responseService.getResponseById(responseIndex);

			// 耕新選題結果
			Long[] selected = new Long[selectedItems.size()];

			for (int i = 0; i < selectedItems.size(); i++) {
				selected[i] = selectedItems.get(i).getId();
			}

			record = new Record();
			record.setSelectedItems(selected);
			record.setInitAbility(initAbility);
			record.setAbility(currentAbility);
			record.setIsFinished(false);

			// 使用Session紀錄作答歷程
			Map<String, Object> sessionMap = ScopeUtil.getScopeAttribute(Scope.SESSION);
			sessionMap.remove("record");
			sessionMap.remove("selectedOptions");
			sessionMap.remove("selectedItems");
			sessionMap.remove("chooseItemPool");
			
			sessionMap.put("record", record);
			sessionMap.put("selectedOptions", selectedOptions);
			sessionMap.put("selectedItems", selectedItems);
			sessionMap.put("chooseItemPool", chooseItemPool);
			
			
			System.out.println("record(init) : " + record);

			return ActionSupport.SUCCESS;
		} else {

			return ActionSupport.ERROR;
		}
	}

	public String chooseItem() {
		System.out.println("chooseItem() begin...");

		// Session取出初始化結果
		Map<String, Object> sessionMap = ScopeUtil.getScopeAttribute(Scope.SESSION);
		@SuppressWarnings("unchecked")
		List<Long> selectedOptions = (List<Long>) sessionMap.get("selectedOptions");
		@SuppressWarnings("unchecked")
		List<Item> selectedItems = (List<Item>) sessionMap.get("selectedItems");
		@SuppressWarnings("unchecked")
		List<Item> chooseItemPool = (List<Item>) sessionMap.get("chooseItemPool");
		
		record = (Record) sessionMap.get("record");
		startTime = (Long) sessionMap.get("startTime");
		isFinished = record.getIsFinished();
		initAbility = record.getInitAbility();
		Double originalAbility = record.getAbility();
		
		if (record.getIsFinished()) {
			isFinished = true;
			return ActionSupport.SUCCESS;
		}

		// get the last time sem
		Double originalReliability = 0d;
		if (record.getSem() != null) {
			originalReliability = 1 - Math.pow(record.getSem(),2);
		}
		
		// 收到選項
		selectedOptions.add(Long.parseLong(selectedOption) + 1);
		//System.out.println("已選擇選項(清單) : " + selectedOptions);

		// 能力估計 
		record = abilityEstimate(selectedItems, originalAbility, selectedOptions);
		
		Double currentReliability = 1 - Math.pow(record.getSem(),2);
		//System.out.println("originalReliability:" + originalReliability);
		//System.out.println("currentReliability:" + currentReliability);
		Double deltaReliability = Math.abs(currentReliability - originalReliability);
		
		// 測驗中止調件(信度 >= 0.9 或 兩次信度差 < 0.001)
		if (currentReliability >= 0.9 || deltaReliability < 0.001) {
			isFinished = true;
			record.setIsFinished(isFinished);
		} else {
			// 設定中止條件，作答二十四題結束
			if (selectedItems.size() >= Parameter.TEST_MAX_ITEM_LENGTH) {
				isFinished = true;
				record.setIsFinished(isFinished);
			} else {
				isFinished = false;
				record.setIsFinished(isFinished);
			}
		}
		
		// 若未結束則繼續選題
		if (!isFinished) {
			// 隨機「選題﹞與「作答反應」
			//item = chooseRandomItem(chooseItemPool);
			item = itemSelection(currentAbility, chooseItemPool, prior);
			selectedItems.add(item);

			// 更新可選題庫
			for (int i = 0 ; i < chooseItemPool.size() ; i++) {
				if (item.getId() == chooseItemPool.get(i).getId()) {
					chooseItemPool.remove(i);
				}
			} 
			
			// 取得所選題目的作答選項與指導語
			Long responseIndex = item.getAnswerType();
			response = responseService.getResponseById(responseIndex);
	
			// 更新選題結果
			Long[] items = new Long[selectedItems.size()];
			for (int i = 0; i < selectedItems.size(); i++) {
				items[i] = selectedItems.get(i).getId();
			}
	
			Long[] options = new Long[selectedOptions.size()];
			selectedOptions.toArray(options);
		}
		
		if (isFinished) {
			// 取得目前紀錄的筆數產生Id
			List<Record> recordList = recordService.getAllRecord();
	
			if (recordList != null && !recordList.isEmpty()) {
				Integer newId = recordList.size();
				record.setId(newId.longValue() + 1);
			} else {
				record.setId(1L);
			}

			// 紀錄測驗完成時間(千分之一秒)
			Long testCompleteTime = new Date().getTime() - startTime;
			record.setTestCompleteTime(testCompleteTime);
			record.setCreateTime(new Date());

			record.setRoleName(((Role) ActionContext.getContext().getSession().get("loginRole")).getUserName());
			
			record.setSubjectName(subjectCode);

			recordService.insertRecord(record);
		}

		System.out.println("record result: " + record);
		
		return ActionSupport.SUCCESS;
	}

	/**
	 * 選題方法
	 * 
	 * @param initAbility
	 * @param itemList
	 * @param prior
	 * @return
	 */
	private Item itemSelection(Double initAbility, List<Item> itemList,
			Double prior) {
		System.out.println("======== Start item selection ========");

		// 初始化參數
		Long chooseItem = 0l; // 初始化所選題目
		Double maxInformation = 0d; // 初始化最大訊息值

		for (Item item : itemList) {
			Double delta = item.getDelta();
			Double step1 = item.getStep1();
			Double step2 = item.getStep2();
			Double step3 = item.getStep3();
			Double step4 = item.getStep4();

			Double prob1 = 1d;
			Double prob2 = Math.exp(1 * initAbility - 1 * delta - (step1));
			Double prob3 = Math.exp(2 * initAbility - 2 * delta
					- (step1 + step2));
			Double prob4 = Math.exp(3 * initAbility - 3 * delta
					- (step1 + step2 + step3));
			Double prob5 = Math.exp(4 * initAbility - 4 * delta
					- (step1 + step2 + step3 + step4));

			Double probSum = prob1 + prob2 + prob3 + prob4 + prob5;

			prob1 = prob1 / probSum;
			prob2 = prob2 / probSum;
			prob3 = prob3 / probSum;
			prob4 = prob4 / probSum;
			prob5 = prob5 / probSum;

			Double betaProb = 0d;
			Double expectK = 0d;
			betaProb = (1 * 1 * prob1) + (2 * 2 * prob2) + (3 * 3 * prob3)
					+ (4 * 4 * prob4) + (5 * 5 * prob5);
			expectK = (1 * prob1) + (2 * prob2) + (3 * prob3)
					+ (4 * prob4) + (5 * prob5);

			Double itemInformation = betaProb - (expectK * expectK) + prior;

			if (itemInformation > maxInformation) {
				maxInformation = itemInformation;
				chooseItem = item.getId();
			}
		}
		System.out.println("Max information : item(" + chooseItem + ")" + ";"
				+ maxInformation);
		System.out.println("======== End item selection ========");

		return itemService.getItemById(chooseItem);
	}

	private Record abilityEstimate(List<Item> itemList, Double initAbility,
			List<Long> optionList) {
		System.out.println("======== Start ability estimate ========");

		// 初始化參數
		Double terminationCriteria = Parameter.TEST_TERMINATION_CRITERIA;
		Double mu = Parameter.TEST_PRIOR_POPULATION_MEAN;
		Double variance = Parameter.TEST_PRIOR_VARIANCE;

		// 初始化迭代參數
		currentAbility = initAbility;
		Double deltaAbility = 1d;
		Double sem = 0d;
		int iterationCount = 1;

		while (Math.abs(deltaAbility) > terminationCriteria) {
			Double sumOfbetaDiffEk = 0d;
			Double sumOfbetaDiffSqEk = 0d;

			for (int i = 0; i < itemList.size(); i++) {
				Long selected = optionList.get(i);

				Double delta = itemList.get(i).getDelta();
				Double step1 = itemList.get(i).getStep1();
				Double step2 = itemList.get(i).getStep2();
				Double step3 = itemList.get(i).getStep3();
				Double step4 = itemList.get(i).getStep4();

				Double prob1 = 1d;
				Double prob2 = Math.exp(1 * currentAbility - 1 * delta
						- (step1));
				Double prob3 = Math.exp(2 * currentAbility - 2 * delta
						- (step1 + step2));
				Double prob4 = Math.exp(3 * currentAbility - 3 * delta
						- (step1 + step2 + step3));
				Double prob5 = Math.exp(4 * currentAbility - 4 * delta
						- (step1 + step2 + step3 + step4));

				Double probSum = prob1 + prob2 + prob3 + prob4 + prob5;

				prob1 = prob1 / probSum;
				prob2 = prob2 / probSum;
				prob3 = prob3 / probSum;
				prob4 = prob4 / probSum;
				prob5 = prob5 / probSum;

				Double betaProb = (1 * 1 * prob1) + (2 * 2 * prob2)
						+ (3 * 3 * prob3) + (4 * 4 * prob4) + (5 * 5 * prob5);
				Double expectK = (1 * prob1) + (2 * prob2) + (3 * prob3)
						+ (4 * prob4) + (5 * prob5);

				sumOfbetaDiffEk += (selected - expectK);
				sumOfbetaDiffSqEk += ((expectK * expectK) - betaProb);
			}

			Double firstOrderDiff = sumOfbetaDiffEk - (currentAbility - mu)
					/ variance;
			Double secondOrderDiff = sumOfbetaDiffSqEk - (1 / variance);

			deltaAbility = firstOrderDiff / secondOrderDiff;
			currentAbility = currentAbility - deltaAbility;

			// 計算SEM
			Double itemInformation = -sumOfbetaDiffSqEk + (1 / variance);
			sem = Math.sqrt(1 / itemInformation);

			iterationCount++;
		}

		// 將所選題號放入陣列紀錄
		Long[] selectedItems = new Long[itemList.size()];
		for (int i = 0; i < itemList.size(); i++) {
			selectedItems[i] = itemList.get(i).getId();
		}

		// 將作答選項放入陣列紀錄
		Long[] selectedOptions = new Long[optionList.size()];
		optionList.toArray(selectedOptions);

		// 寫入紀錄
		record.setAbility(currentAbility);
		record.setSem(sem);
		record.setSelectedItems(selectedItems);
		record.setSelectedOptions(selectedOptions);
		
		System.out.println("======== End ability estimate ========");

		return record;
	}

	public String getSelectedOption() {
		return selectedOption;
	}

	public void setSelectedOption(String selectedOption) {
		this.selectedOption = selectedOption;
	}

	public Item getItem() {
		return item;
	}

	public void setItem(Item item) {
		this.item = item;
	}

	public Response getResponse() {
		return response;
	}

	public void setResponse(Response response) {
		this.response = response;
	}

	public Boolean getIsFinished() {
		return isFinished;
	}

	public void setIsFinished(Boolean isFinished) {
		this.isFinished = isFinished;
	}

	public Record getRecord() {
		return record;
	}

	public void setRecord(Record record) {
		this.record = record;
	}
	
	public List<Record> getRecordList() {
		return recordList;
	}

	public void setRecordList(List<Record> recordList) {
		this.recordList = recordList;
	}

	public String getSubjectCode() {
		return subjectCode;
	}

	public void setSubjectCode(String subjectCode) {
		this.subjectCode = subjectCode;
	}

}
