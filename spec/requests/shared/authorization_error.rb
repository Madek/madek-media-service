RSpec.configure do |c|
  c.alias_it_should_behave_like_to :it_raises
end

shared_examples "authorization error" do
  let(:api_token) do
    create(:api_token, user: user, scope_write: true) if user
  end
  let(:user_token) { api_token&.token_hash }

  it "responds with 500" do
    expect(response.status).to eq(500)
  end

  it "responds with error message" do
    expect(response.body).to eq("System-admin scope required")
  end
end
