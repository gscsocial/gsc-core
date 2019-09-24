contract CpuOfTransferFailedTest {
    constructor() payable public {

    }

    function testTransferTokenCompiledLongMax() payable public{
            address(0x1).transferToken(1,9223372036855775827);
    }

    function testTransferTokenCompiled() payable public{
        address(0x1).transferToken(1,1);
    }

    function testTransferTokenCompiledLongMin() payable public{
        //address(0x1).transferToken(1,-9223372036855775828);
    }

    function testTransferTokenCompiledLongMin1() payable public returns(uint256){
        return address(0x2).tokenBalance(grcToken(-9223372036855775828));
    }

    function testTransferTokenCompiled1() payable public returns(uint256){
        return address(0x1).tokenBalance(grcToken(1));
    }

    function testTransferTokenCompiledLongMax1() payable public returns(uint256){
        return address(0x2).tokenBalance(grcToken(9223372036855775827));
    }

    function testTransferTokenCompiledTokenId(uint256 tokenid) payable public returns(uint256){
         return address(0x1).tokenBalance(grcToken(tokenid));
    }

    function testTransferTokenTest(address addr ,uint256 tokenid) payable public returns(uint256){
          return  addr.tokenBalance(grcToken(tokenid));
    }

    // InsufficientBalance
    function testTransferGscInsufficientBalance(uint256 i) payable public{
        msg.sender.transfer(i);
    }

    function testSendGscInsufficientBalance(uint256 i) payable public{
        msg.sender.send(i);
    }

    function testTransferTokenInsufficientBalance(uint256 i,grcToken tokenId) payable public{
        msg.sender.transferToken(i, tokenId);
    }

    function testCallGscInsufficientBalance(uint256 i,address payable caller) public {
        caller.call.value(i)(abi.encodeWithSignature("test()"));
    }

    function testCreateGscInsufficientBalance(uint256 i) payable public {
        (new Caller).value(i)();
    }

    // NonexistentTarget

    function testSendGscNonexistentTarget(uint256 i,address payable nonexistentTarget) payable public {
        nonexistentTarget.send(i);
    }

    function testTransferGscNonexistentTarget(uint256 i,address payable nonexistentTarget) payable public {
        nonexistentTarget.transfer(i);
    }

    function testTransferTokenNonexistentTarget(uint256 i,address payable nonexistentTarget, grcToken tokenId) payable public {
        nonexistentTarget.transferToken(i, tokenId);
    }

    function testCallGscNonexistentTarget(uint256 i,address payable nonexistentTarget) payable public {
        nonexistentTarget.call.value(i)(abi.encodeWithSignature("test()"));
    }

    function testSuicideNonexistentTarget(address payable nonexistentTarget) payable public {
         selfdestruct(nonexistentTarget);
    }

    // target is self
    function testTransferGscSelf(uint256 i) payable public{
        address payable self = address(uint160(address(this)));
        self.transfer(i);
    }

    function testSendGscSelf(uint256 i) payable public{
        address payable self = address(uint160(address(this)));
        self.send(i);
    }

    function testTransferTokenSelf(uint256 i,grcToken tokenId) payable public{
        address payable self = address(uint160(address(this)));
        self.transferToken(i, tokenId);
    }

    event Deployed(address addr, uint256 salt, address sender);
            function deploy(bytes memory code, uint256 salt) public returns(address){
                address addr;
                assembly {
                    addr := create2(10, add(code, 0x20), mload(code), salt)
                    //if iszero(extcodesize(addr)) {
                    //    revert(0, 0)
                    //}
                }
                //emit Deployed(addr, salt, msg.sender);
                return addr;
            }
            function deploy2(bytes memory code, uint256 salt) public returns(address){
                    address addr;
                    assembly {
                        addr := create2(300, add(code, 0x20), mload(code), salt)
                        //if iszero(extcodesize(addr)) {
                        //    revert(0, 0)
                        //}
                    }
                    //emit Deployed(addr, salt, msg.sender);
                    return addr;
                }
}



contract Caller {
    constructor() payable public {}
    function test() payable public {}
}