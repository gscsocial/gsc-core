//pragma solidity ^0.4.24;

contract token{
    address public a;
    constructor() public payable{}
    function transferTokenWithSameName(grcToken id,uint256 amount) public payable{
        B b= new B();
        b.transferToken(amount,id);
        a= address(b);
    }
}


contract B{
    uint256 public  flag =0;
    constructor() public payable{}
    function() external payable{}
    function transferToken(uint256 amount, grcToken id) payable public returns(bool){
        flag =9;
    }
    function getFlag() public view returns (uint256){
        return flag;
    }
}