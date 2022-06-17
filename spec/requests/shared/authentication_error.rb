RSpec.configure do |c|
  c.alias_it_should_behave_like_to :it_raises
end

shared_examples "authentication error" do
  let(:api_token) do
    create(:api_token, user: user, scope_write: true) if user
  end
  let(:user_token) { api_token&.token_hash }

  it "responds with 401 Forbidden status" do
    expect(response.status).to eq(401)
  end

  it "responds with error message" do
    expect(response.body).to include("Unauthorized - Authentication/Sign-in required")
  end
end
