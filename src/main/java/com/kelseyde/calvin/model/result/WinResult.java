package com.kelseyde.calvin.model.result;

import com.kelseyde.calvin.model.WinType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WinResult extends GameResult {

    private final ResultType resultType = ResultType.WIN;

    private final boolean isWhiteWinner;

    private final WinType winType;

}
