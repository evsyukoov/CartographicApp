package ru.evsyukoov.transform.stateMachine;

public enum State {

    INPUT,
    CHOOSE_TRANSFORMATION_TYPE,
    CHOOSE_OUTPUT_FILE_OPTION,
    CHOOSE_SYSTEM_COORDINATE_SRC,
    CHOOSE_SYSTEM_COORDINATE_TGT,
    TRANSFORM,
    HELP
}
