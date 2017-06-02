package com.pousheng.erp.rules;

import com.google.common.base.CharMatcher;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.pousheng.erp.model.SkuGroupRule;

import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-05-23
 */
public enum SkuGroupRuler {

    SPLIT(1) {
        @Override
        public String spuCode(SkuGroupRule skuGroupRule, String materialCode) {
            Character splitChar = skuGroupRule.getSplitChar();

            List<String> parts = Splitter.on(splitChar).omitEmptyStrings().trimResults().splitToList(materialCode);
            return parts.get(0);
        }
    },

    INDEX(2) {
        @Override
        public String spuCode(SkuGroupRule skuGroupRule, String materialCode) {
            Integer lastStart = skuGroupRule.getLastStart();
            int length = materialCode.length();
            return materialCode.substring(0, length - lastStart);
        }
    },

    MIX(3){//如果materialCode中含有分隔符, 则优先使用分隔符, 否则使用xx位
        @Override
        public String spuCode(SkuGroupRule skuGroupRule, String materialCode) {
            if(CharMatcher.is(skuGroupRule.getSplitChar()).matchesAnyOf(materialCode)){
                return SPLIT.spuCode(skuGroupRule, materialCode);
            }else{
                return INDEX.spuCode(skuGroupRule, materialCode);
            }
        }
    };

    public final int value;

    SkuGroupRuler(int value) {
        this.value = value;
    }


    public static SkuGroupRuler from(int value){
        for (SkuGroupRuler skuGroupRuler : SkuGroupRuler.values()) {
            if(skuGroupRuler.value == value){
                return skuGroupRuler;
            }
        }
        throw new IllegalArgumentException("unknown rule type: "+value);
    }



    public abstract String spuCode(SkuGroupRule skuGroupRule, String materialCode);

    public boolean support(SkuGroupRule skuGroupRule, String cardId, String kindId) {
        //判断品牌id是否匹配
        if (!Objects.equal(cardId, skuGroupRule.getCardId())) {
            return false;
        }

        //判断类目id是否匹配(如果存在类目id)
        if (skuGroupRule.getKindId() != null
                && !Objects.equal(skuGroupRule.getKindId(), kindId)) {
            return false;

        }
        return true;
    }
}
