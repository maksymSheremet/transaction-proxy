package my.code.transactionproxy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionDTO {
    private Long id;
    private String doc_vob;
    private String doc_vob_name;
    private String doc_number;
    private LocalDate doc_date;
    private LocalDate doc_v_date;
    private LocalDate trans_date;
    private Double amount;
    private Long amount_cop;
    private String currency;

    private String payer_edrpou;
    private String payer_name;
    private String payer_account;
    private String payer_mfo;
    private String payer_bank;
    private String payer_edrpou_fact;
    private String payer_name_fact;

    private String recipt_edrpou;
    private String recipt_name;
    private String recipt_account;
    private String recipt_mfo;
    private String recipt_bank;
    private String recipt_edrpou_fact;
    private String recipt_name_fact;

    private String payment_details;
    private String doc_add_attr;
    private Integer region_id;
    private String payment_type;
    private String payment_data;
    private Integer source_id;
    private String source_name;
    private Integer kekv;
    private String kpk;
    private String contractId;
    private String contractNumber;
    private String budgetCode;
    private String system_key;
    private String system_key_ff;
}